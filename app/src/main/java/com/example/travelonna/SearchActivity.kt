package com.example.travelonna

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.AccountSearchAdapter
import com.example.travelonna.adapter.PlaceAdapter
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.SearchResponse
import com.example.travelonna.api.FollowStatusResponse
import com.example.travelonna.model.Place
import com.example.travelonna.model.Post
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : BaseActivity() {

    companion object {
        private const val TAG = "SearchActivity"
    }

    private lateinit var backButton: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var placeRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noResultsText: TextView
    private lateinit var recentSearchesTitle: TextView
    private lateinit var popularSearchesTitle: TextView
    private lateinit var recentSearchesContainer: LinearLayout
    private lateinit var popularSearchItem1: TextView
    private lateinit var popularSearchItem2: TextView
    private lateinit var popularSearchItem3: TextView
    private lateinit var popularSearchItem4: TextView
    private lateinit var popularSearchItem5: TextView
    private lateinit var tabLayout: LinearLayout
    private lateinit var placeTab: LinearLayout
    private lateinit var accountTab: LinearLayout
    private lateinit var placeTabText: TextView
    private lateinit var accountTabText: TextView
    private lateinit var placeTabIndicator: View
    private lateinit var accountTabIndicator: View
    
    // 검색 타입 (장소 또는 계정)
    private enum class SearchType { PLACE, ACCOUNT }
    private var currentSearchType = SearchType.PLACE
    
    // 최근 검색어 저장을 위한 SharedPreferences 키
    private val PREFS_NAME = "SearchPrefs"
    private val RECENT_SEARCHES_KEY = "recent_searches"
    private val MAX_RECENT_SEARCHES = 5
    
    private lateinit var postAdapter: AccountSearchAdapter
    private lateinit var placeAdapter: PlaceAdapter
    private lateinit var postDetailLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // 하단 네비게이션 바 설정
        setupBottomNavBar(R.id.navSearch)
        
        // ActivityResultLauncher 초기화
        setupActivityResultLauncher()
        
        // 뷰 초기화
        initViews()
        
        // 어댑터 초기화 (클릭 리스너 포함)
        postAdapter = AccountSearchAdapter(listOf()) { post ->
            if (post.date == "사용자") {
                // 유저 로그 목록 화면으로 이동
                val intent = Intent(this, UserLogListActivity::class.java)
                intent.putExtra(UserLogListActivity.EXTRA_USER_ID, post.id)
                startActivity(intent)
            } else {
                // 로그 상세로 이동
                if (post.id <= 0) {
                    Toast.makeText(this, "유효하지 않은 게시글입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Opening PostDetailActivity with logId: ${post.id}")
                    val intent = Intent(this, PostDetailActivity::class.java)
                    intent.putExtra("EXTRA_POST_ID", post.id)
                    postDetailLauncher.launch(intent)
                }
            }
        }
        placeAdapter = PlaceAdapter(listOf()) { place ->
            val intent = Intent(this, PlacePostListActivity::class.java)
            intent.putExtra("placeId", place.id)
            startActivity(intent)
        }
        
        // 리스너 설정
        setupListeners()
        
        // 리사이클러뷰 설정
        setupRecyclerViews()
        
        // 초기 상태 설정
        showInitialState()
    }
    
    private fun setupActivityResultLauncher() {
        postDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val needsRefresh = result.data?.getBooleanExtra(PostDetailActivity.RESULT_REFRESH_NEEDED, false) ?: false
                if (needsRefresh) {
                    Log.d(TAG, "PostDetail returned with refresh needed, performing search again")
                    val currentQuery = searchEditText.text.toString().trim()
                    if (currentQuery.isNotEmpty()) {
                        performSearch(currentQuery)
                    }
                }
            }
        }
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.searchIcon)
        recyclerView = findViewById(R.id.searchResultsRecyclerView)
        placeRecyclerView = findViewById(R.id.placeRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        noResultsText = findViewById(R.id.noResultsText)
        recentSearchesTitle = findViewById(R.id.recentSearchesTitle)
        popularSearchesTitle = findViewById(R.id.popularSearchesTitle)
        recentSearchesContainer = findViewById(R.id.recentSearchesContainer)
        popularSearchItem1 = findViewById(R.id.popularSearchItem1)
        popularSearchItem2 = findViewById(R.id.popularSearchItem2)
        popularSearchItem3 = findViewById(R.id.popularSearchItem3)
        popularSearchItem4 = findViewById(R.id.popularSearchItem4)
        popularSearchItem5 = findViewById(R.id.popularSearchItem5)
        tabLayout = findViewById(R.id.tabLayout)
        placeTab = findViewById(R.id.placeTab)
        accountTab = findViewById(R.id.accountTab)
        placeTabText = findViewById(R.id.placeTabText)
        accountTabText = findViewById(R.id.accountTabText)
        placeTabIndicator = findViewById(R.id.placeTabIndicator)
        accountTabIndicator = findViewById(R.id.accountTabIndicator)
    }
    
    private fun setupListeners() {
        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }
        
        // 텍스트 입력 리스너 설정
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty() || s.length < 2) {
                    // 검색창이 비어있거나 2글자 미만일 때 즉시 초기 상태로 복귀
                    clearSearchResults()
                    showInitialState()
                    return
                }
                
                // 검색창에 2글자 이상 입력 시 검색 시작
                performSearch(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 검색 버튼(키보드) 클릭 설정
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    saveRecentSearch(query)
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }
        
        // 검색 아이콘 클릭 설정
        searchIcon.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                saveRecentSearch(query)
                performSearch(query)
            }
        }

        // 인기 검색어 클릭 이벤트
        val popularSearchItems = listOf(
            popularSearchItem1,
            popularSearchItem2, 
            popularSearchItem3,
            popularSearchItem4,
            popularSearchItem5
        )
        
        popularSearchItems.forEach { item ->
            item.setOnClickListener {
                val keyword = item.text.toString()
                searchEditText.setText(keyword)
                saveRecentSearch(keyword)
                performSearch(keyword)
            }
        }
        
        // 탭 클릭 이벤트
        placeTab.setOnClickListener {
            if (currentSearchType != SearchType.PLACE) {
                setCurrentTab(SearchType.PLACE)
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
        }
        
        accountTab.setOnClickListener {
            if (currentSearchType != SearchType.ACCOUNT) {
                setCurrentTab(SearchType.ACCOUNT)
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
        }
    }
    
    private fun setupRecyclerViews() {
        // 검색 결과 리사이클러뷰 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter
        
        // 장소 그리드 리사이클러뷰 설정
        placeRecyclerView.layoutManager = GridLayoutManager(this, 2)
        placeRecyclerView.adapter = placeAdapter
    }
    
    // 실제 API 검색 수행
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            clearSearchResults()
            showInitialState()
            return
        }

        // 초기 화면 요소 숨기기
        hideInitialState()
        
        // 탭 레이아웃 보이기
        tabLayout.visibility = View.VISIBLE
        
        // 로딩 상태 표시
        showLoading()
        
        Log.d(TAG, "Performing search with keyword: $query")
        
        // API 호출
        RetrofitClient.apiService.search(query).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                hideLoading()
                
                if (response.isSuccessful && response.body() != null) {
                    val searchResponse = response.body()!!
                    Log.d(TAG, "Search API success: ${searchResponse.success}")
                    
                    if (searchResponse.success) {
                        handleSearchResults(searchResponse)
                    } else {
                        Log.e(TAG, "Search API returned success=false: ${searchResponse.message}")
                        showNoResults(true)
                        Toast.makeText(this@SearchActivity, searchResponse.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Search API failed: ${response.code()}, ${response.message()}")
                    Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
                    showNoResults(true)
                    Toast.makeText(this@SearchActivity, "검색에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                hideLoading()
                Log.e(TAG, "Search API network error", t)
                showNoResults(true)
                Toast.makeText(this@SearchActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    // 검색 결과 처리
    private fun handleSearchResults(searchResponse: SearchResponse) {
        val data = searchResponse.data
        
        when (currentSearchType) {
            SearchType.PLACE -> {
                // 장소 검색 결과 처리
                val places = data.places.map { searchPlace ->
                    Place(
                        id = searchPlace.placeId.toLong(),
                        name = searchPlace.name,
                        address = searchPlace.address,
                        imageResource = R.drawable.placeholder_image // 기본 이미지
                    )
                }
                
                placeAdapter.updateData(places)
                setCurrentTab(SearchType.PLACE)
                showNoResults(places.isEmpty())
                
                // 검색 결과가 있을 때 placeRecyclerView를 보이게 설정
                if (places.isNotEmpty()) {
                    placeRecyclerView.visibility = View.VISIBLE
                }
                
                Log.d(TAG, "Places found: ${places.size}")
            }
            
            SearchType.ACCOUNT -> {
                val posts = mutableListOf<Post>()

                // 사용자 검색 결과를 Post로 변환
                data.users.forEach { user ->
                    val displayName = if (user.nickname.isNotBlank()) {
                        user.nickname
                    } else {
                        "사용자_${user.userId}" // nickname이 비어있으면 fallback
                    }
                    
                    Log.d(TAG, "사용자 검색 결과 - userId: ${user.userId}, nickname: '${user.nickname}', displayName: '$displayName'")
                    
                    posts.add(Post(
                        id = user.userId.toLong(),
                        userName = displayName,
                        isFollowing = false, // 기본값으로 설정, 이후 실제 상태를 조회
                        description = user.introduction ?: "",
                        imageResource = R.drawable.default_profile,
                        date = "사용자",
                        likeCount = 0,
                        commentCount = 0,
                        isLiked = false
                    ))
                }
                
                // 각 사용자의 실제 팔로우 상태 조회
                loadFollowStatusForUsers(posts.filter { it.date == "사용자" })

                // users가 있을 때만 logs도 추가
                if (data.users.isNotEmpty()) {
                    data.logs.forEach { log ->
                        val logId = log.logId
                        if (logId != null && logId > 0) {
                            Log.d(TAG, "Processing log: id=$logId, userName=${log.userName}, comment=${log.comment}, createdAt=${log.createdAt}")
                            posts.add(Post(
                                id = logId.toLong(),
                                userName = log.userName ?: "",
                                isFollowing = false,
                                description = log.comment ?: "",
                                imageResource = R.drawable.placeholder_image,
                                date = if (!log.createdAt.isNullOrEmpty() && log.createdAt.length >= 10) log.createdAt.substring(0, 10) else "",
                                likeCount = log.likeCount ?: 0,
                                commentCount = 0,
                                isLiked = false
                            ))
                        } else {
                            Log.w(TAG, "Skipping log with invalid id: $logId")
                        }
                    }
                }

                postAdapter.updateData(posts)
                setCurrentTab(SearchType.ACCOUNT)
                showNoResults(posts.isEmpty())
                Log.d(TAG, "Users found: ${data.users.size}, Logs found: ${data.logs.size}, Valid logs added: ${posts.size - data.users.size}")
            }
        }
    }
    
    // 현재 선택된 탭 설정
    private fun setCurrentTab(searchType: SearchType) {
        currentSearchType = searchType
        
        if (searchType == SearchType.PLACE) {
            // 장소 탭 활성화
            placeTabText.setTextColor(resources.getColor(android.R.color.black, null))
            accountTabText.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            placeTabIndicator.visibility = View.VISIBLE
            accountTabIndicator.visibility = View.INVISIBLE
            placeRecyclerView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // 계정 탭 활성화
            placeTabText.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            accountTabText.setTextColor(resources.getColor(android.R.color.black, null))
            placeTabIndicator.visibility = View.INVISIBLE
            accountTabIndicator.visibility = View.VISIBLE
            placeRecyclerView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    // 검색 결과 초기화
    private fun clearSearchResults() {
        // 즉시 모든 검색 관련 뷰 숨기기
        tabLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
        placeRecyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE
        progressBar.visibility = View.GONE
        
        // 즉시 어댑터 데이터 초기화
        postAdapter.updateData(emptyList())
        placeAdapter.updateData(emptyList())
    }
    
    // 초기 상태 (최근/인기 검색어 표시)
    private fun showInitialState() {
        // 초기 화면 요소 표시
        recentSearchesTitle.visibility = View.VISIBLE
        popularSearchesTitle.visibility = View.VISIBLE
        recentSearchesContainer.visibility = View.VISIBLE
        
        // 인기 검색어 표시
        popularSearchItem1.visibility = View.VISIBLE
        popularSearchItem2.visibility = View.VISIBLE
        popularSearchItem3.visibility = View.VISIBLE
        popularSearchItem4.visibility = View.VISIBLE
        popularSearchItem5.visibility = View.VISIBLE
        
        // 최근 검색어 로드 및 표시
        loadRecentSearches()
    }
    
    // 초기 상태 숨기기
    private fun hideInitialState() {
        recentSearchesTitle.visibility = View.GONE
        popularSearchesTitle.visibility = View.GONE
        recentSearchesContainer.visibility = View.GONE
        
        // 인기 검색어 숨기기
        popularSearchItem1.visibility = View.GONE
        popularSearchItem2.visibility = View.GONE
        popularSearchItem3.visibility = View.GONE
        popularSearchItem4.visibility = View.GONE
        popularSearchItem5.visibility = View.GONE
    }
    
    // 로딩 상태 표시
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        placeRecyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE
    }
    
    // 로딩 상태 숨기기
    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }
    
    // 검색 결과 없음 표시
    private fun showNoResults(show: Boolean) {
        noResultsText.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            placeRecyclerView.visibility = View.GONE
        }
    }
    
    // 최근 검색어 저장
    private fun saveRecentSearch(query: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentSearches = getRecentSearches().toMutableList()
        
        // 이미 같은 검색어가 있다면 제거
        recentSearches.remove(query)
        
        // 최근 검색어 목록 맨 앞에 추가
        recentSearches.add(0, query)
        
        // 최대 개수 유지
        if (recentSearches.size > MAX_RECENT_SEARCHES) {
            recentSearches.removeAt(recentSearches.lastIndex)
        }
        
        // 저장
        prefs.edit().putString(RECENT_SEARCHES_KEY, recentSearches.joinToString(",")).apply()
        
        // UI 업데이트
        loadRecentSearches()
    }
    
    // 최근 검색어 목록 가져오기
    private fun getRecentSearches(): List<String> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentSearchesStr = prefs.getString(RECENT_SEARCHES_KEY, "") ?: ""
        return if (recentSearchesStr.isEmpty()) emptyList() else recentSearchesStr.split(",")
    }
    
    // 최근 검색어 로드 및 UI에 표시
    private fun loadRecentSearches() {
        recentSearchesContainer.removeAllViews()
        val recentSearches = getRecentSearches()
        
        if (recentSearches.isEmpty()) {
            // 최근 검색어가 없을 경우 기본 텍스트 표시
            val noRecentTextView = TextView(this)
            noRecentTextView.text = "최근 검색 내역이 없습니다"
            noRecentTextView.textSize = 15f
            noRecentTextView.setTextColor(resources.getColor(android.R.color.black, null))
            noRecentTextView.setPadding(
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                resources.getDimensionPixelSize(R.dimen.activity_vertical_margin),
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
            )
            recentSearchesContainer.addView(noRecentTextView)
        } else {
            // 수평 스크롤을 위한 HorizontalScrollView 추가
            val horizontalScrollView = HorizontalScrollView(this)
            horizontalScrollView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // 수평 방향으로 아이템을 배치할 LinearLayout
            val horizontalLayout = LinearLayout(this)
            horizontalLayout.orientation = LinearLayout.HORIZONTAL
            horizontalLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // 최근 검색어 표시
            for (searchTerm in recentSearches) {
                val searchItemView = TextView(this)
                searchItemView.text = searchTerm
                searchItemView.textSize = 15f
                searchItemView.setTextColor(resources.getColor(android.R.color.black, null))
                searchItemView.setPadding(16, 8, 16, 8)
                searchItemView.background = resources.getDrawable(R.drawable.search_item_bg, null)
                
                // 검색어 클릭 이벤트
                searchItemView.setOnClickListener {
                    searchEditText.setText(searchTerm)
                    saveRecentSearch(searchTerm) // 클릭 시 맨 위로 이동
                    performSearch(searchTerm)
                }
                
                // Margins 설정을 위해 LayoutParams 사용
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(0, 0, 16, 0) // 오른쪽 마진 16dp
                searchItemView.layoutParams = layoutParams
                
                horizontalLayout.addView(searchItemView)
            }
            
            horizontalScrollView.addView(horizontalLayout)
            recentSearchesContainer.addView(horizontalScrollView)
        }
    }



    /**
     * 검색된 사용자들의 실제 팔로우 상태를 서버에서 조회
     */
    private fun loadFollowStatusForUsers(userPosts: List<Post>) {
        userPosts.forEach { post ->
            val userId = post.id.toInt()
            
            // 자기 자신은 팔로우 상태 조회하지 않음
            val currentUserId = RetrofitClient.getUserId()
            if (currentUserId == userId) {
                return@forEach
            }
            
            Log.d(TAG, "팔로우 상태 조회 시작 - userId: $userId")
            RetrofitClient.apiService.getFollowStatus(userId).enqueue(object : Callback<FollowStatusResponse> {
                override fun onResponse(call: Call<FollowStatusResponse>, response: Response<FollowStatusResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val statusResponse = response.body()!!
                        
                        if (statusResponse.success && statusResponse.data != null) {
                            val isFollowing = statusResponse.data.isFollowing
                            Log.d(TAG, "팔로우 상태 조회 성공 - userId: $userId, isFollowing: $isFollowing")
                            
                            // Post 객체의 팔로우 상태 업데이트
                            post.isFollowing = isFollowing
                            
                                                         // 어댑터에 변경사항 알림
                             val position = postAdapter.getPosts().indexOf(post)
                             if (position >= 0) {
                                 postAdapter.notifyItemChanged(position)
                             }
                        } else {
                            Log.w(TAG, "팔로우 상태 조회 실패 - userId: $userId, message: ${statusResponse.message}")
                        }
                    } else {
                        Log.e(TAG, "팔로우 상태 조회 HTTP 오류 - userId: $userId, code: ${response.code()}")
                    }
                }
                
                override fun onFailure(call: Call<FollowStatusResponse>, t: Throwable) {
                    Log.e(TAG, "팔로우 상태 조회 네트워크 오류 - userId: $userId", t)
                }
            })
        }
    }
} 