package com.example.travelonna

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.AccountSearchAdapter
import com.example.travelonna.adapter.PlaceAdapter
import com.example.travelonna.model.Place
import com.example.travelonna.model.Post

class SearchActivity : AppCompatActivity() {

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
    
    // 임시 데이터 - 실제로는 API에서 받아와야 함
    private val allPosts = mutableListOf(
        Post(
            id = 1L,
            imageResource = R.drawable.main_dummy_1,
            userName = "seoul_traveler",
            isFollowing = true,
            description = "서울 N서울타워에서 본 야경이 정말 아름다웠어요. 연인과 함께 방문하기 좋은 곳!",
            date = "2023.10.15"
        ),
        Post(
            id = 2L,
            imageResource = R.drawable.main_dummy_2,
            userName = "jeju_explorer",
            isFollowing = false,
            description = "제주도 성산일출봉에서 맞이한 아침. 일출 명소로 정말 추천해요!",
            date = "2023.09.22"
        ),
        Post(
            id = 3L,
            imageResource = R.drawable.main_dummy_1,
            userName = "busan_lover",
            isFollowing = true,
            description = "부산 해운대 해변에서의 휴가. 날씨도 좋고 바다도 정말 깨끗했습니다.",
            date = "2023.08.10"
        ),
        Post(
            id = 4L,
            imageResource = R.drawable.main_dummy_2,
            userName = "korea_trip",
            isFollowing = false,
            description = "경복궁 야간 개장! 밤에 보는 고궁의 모습은 또 다른 매력이 있네요.",
            date = "2023.07.05"
        ),
        Post(
            id = 5L,
            imageResource = R.drawable.main_dummy_1,
            userName = "travel_with_me",
            isFollowing = true,
            description = "전주 한옥마을에서의 하루. 한복 입고 거리 구경하기 너무 좋았어요!",
            date = "2023.06.17"
        ),
        Post(
            id = 6L,
            imageResource = R.drawable.main_dummy_2,
            userName = "photo_journey",
            isFollowing = false,
            description = "인사동 문화거리에서 전통 공예품 구경. 외국인 친구들이 정말 좋아했어요.",
            date = "2023.05.30"
        ),
        Post(
            id = 7L,
            imageResource = R.drawable.main_dummy_1,
            userName = "hiking_korea",
            isFollowing = true,
            description = "북한산 등산 코스 추천! 정상에서 본 서울 전경이 일품입니다.",
            date = "2023.04.12"
        ),
        Post(
            id = 8L,
            imageResource = R.drawable.main_dummy_2,
            userName = "food_traveler",
            isFollowing = false,
            description = "명동에서 꼭 먹어봐야 할 길거리 음식 TOP 5! 오뎅부터 호떡까지~",
            date = "2023.03.25"
        ),
        Post(
            id = 9L,
            imageResource = R.drawable.main_dummy_1,
            userName = "korean_culture",
            isFollowing = true,
            description = "북촌 한옥마을에서의 전통 문화 체험. 한복 입고 사진 찍기 추천!",
            date = "2023.02.18"
        ),
        Post(
            id = 10L,
            imageResource = R.drawable.main_dummy_2,
            userName = "seoul_night",
            isFollowing = false,
            description = "서울의 야경 명소들. 한강 야경부터 남산타워까지!",
            date = "2023.01.30"
        ),
        Post(
            id = 11L,
            imageResource = R.drawable.main_dummy_1,
            userName = "jeju_guide",
            isFollowing = true,
            description = "제주도 여행 가이드. 숨은 명소부터 맛집까지 소개합니다.",
            date = "2022.12.15"
        ),
        Post(
            id = 12L,
            imageResource = R.drawable.main_dummy_2,
            userName = "busan_foodie",
            isFollowing = false,
            description = "부산 맛집 투어! 회부터 돼지국밥까지 부산의 맛을 소개합니다.",
            date = "2022.11.20"
        ),
        Post(
            id = 13L,
            imageResource = R.drawable.main_dummy_1,
            userName = "korea_photographer",
            isFollowing = true,
            description = "한국의 아름다운 풍경들. 사진으로 담은 한국의 매력.",
            date = "2022.10.05"
        ),
        Post(
            id = 14L,
            imageResource = R.drawable.main_dummy_2,
            userName = "seoul_shopping",
            isFollowing = false,
            description = "서울 쇼핑 가이드. 동대문부터 명동까지 쇼핑 명소 소개!",
            date = "2022.09.12"
        ),
        Post(
            id = 15L,
            imageResource = R.drawable.main_dummy_1,
            userName = "korean_festival",
            isFollowing = true,
            description = "한국의 전통 축제들. 계절별 축제 정보와 체험 후기.",
            date = "2022.08.25"
        ),
        Post(
            id = 16L,
            imageResource = R.drawable.main_dummy_2,
            userName = "jeju_cafe",
            isFollowing = false,
            description = "제주도 카페 투어. 바다가 보이는 카페부터 숨은 명소까지!",
            date = "2022.07.30"
        )
    )
    
    // 장소 더미 데이터
    private val allPlaces = mutableListOf(
        Place(
            id = 1L,
            name = "서울타워 N서울타워",
            imageResource = R.drawable.main_dummy_1,
            address = "서울특별시 용산구 남산공원길 105",
            rating = 4.5f,
            distance = "2.3km"
        ),
        Place(
            id = 2L,
            name = "경복궁",
            imageResource = R.drawable.main_dummy_2,
            address = "서울특별시 종로구 사직로 161",
            rating = 4.7f,
            distance = "1.8km"
        ),
        Place(
            id = 4L,
            name = "부산 해운대 해변",
            imageResource = R.drawable.main_dummy_2,
            address = "부산광역시 해운대구 해운대해변로",
            rating = 4.6f,
            distance = "0.5km"
        ),
        Place(
            id = 5L,
            name = "인사동 문화거리",
            imageResource = R.drawable.main_dummy_1,
            address = "서울특별시 종로구 인사동길",
            rating = 4.4f,
            distance = "1.2km"
        ),
        Place(
            id = 6L,
            name = "광안대교",
            imageResource = R.drawable.main_dummy_2,
            address = "부산광역시 수영구 광안해변로 219",
            rating = 4.7f,
            distance = "0.8km"
        ),
        Place(
            id = 7L,
            name = "전주 한옥마을",
            imageResource = R.drawable.main_dummy_1,
            address = "전라북도 전주시 완산구 기린대로 99",
            rating = 4.9f,
            distance = "2.1km"
        ),
        Place(
            id = 8L,
            name = "경주 불국사",
            imageResource = R.drawable.main_dummy_2,
            address = "경상북도 경주시 불국로 385",
            rating = 4.8f,
            distance = "3.7km"
        ),
        Place(
            id = 9L,
            name = "에버랜드",
            imageResource = R.drawable.main_dummy_1,
            address = "경기도 용인시 처인구 포곡읍 에버랜드로 199",
            rating = 4.6f,
            distance = "4.2km"
        ),
        Place(
            id = 10L,
            name = "롯데월드",
            imageResource = R.drawable.main_dummy_2,
            address = "서울특별시 송파구 올림픽로 240",
            rating = 4.5f,
            distance = "1.5km"
        ),
        Place(
            id = 11L,
            name = "강릉 안목해변",
            imageResource = R.drawable.main_dummy_1,
            address = "강원도 강릉시 주문진읍 안목해변길",
            rating = 4.7f,
            distance = "0.3km"
        ),
        Place(
            id = 12L,
            name = "남산공원",
            imageResource = R.drawable.main_dummy_2,
            address = "서울특별시 용산구 남산공원길 105",
            rating = 4.4f,
            distance = "1.7km"
        ),
        Place(
            id = 13L,
            name = "북촌 한옥마을",
            imageResource = R.drawable.main_dummy_1,
            address = "서울특별시 종로구 계동길 37",
            rating = 4.6f,
            distance = "2.4km"
        ),
        Place(
            id = 14L,
            name = "동대문디자인플라자",
            imageResource = R.drawable.main_dummy_2,
            address = "서울특별시 중구 을지로 281",
            rating = 4.5f,
            distance = "1.9km"
        ),
        Place(
            id = 15L,
            name = "한강공원",
            imageResource = R.drawable.main_dummy_1,
            address = "서울특별시 영등포구 여의대로",
            rating = 4.8f,
            distance = "0.6km"
        ),
        Place(
            id = 16L,
            name = "명동 쇼핑거리",
            imageResource = R.drawable.main_dummy_2,
            address = "서울특별시 중구 명동길",
            rating = 4.3f,
            distance = "1.1km"
        )
    )
    
    private val postAdapter = AccountSearchAdapter(listOf())
    private val placeAdapter = PlaceAdapter(listOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // 뷰 초기화
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

        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }
        
        // 텍스트 입력 리스너 설정
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    // 검색창이 비어있을 때 즉시 초기 상태로 복귀
                    clearSearchResults()
                    showInitialState()
                    return
                }
                
                if (s.length >= 2) {
                    // 검색창에 2글자 이상 입력 시 검색 시작
                    showSearchResultsWithoutSaving(s.toString())
                } else {
                    // 2글자 미만일 때는 즉시 초기 상태로 복귀
                    clearSearchResults()
                    showInitialState()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 검색 버튼(키보드) 클릭 설정
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    saveRecentSearch(query)
                    showSearchResults(query)
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
                showSearchResults(query)
            }
        }

        // 검색 결과 리사이클러뷰 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter
        
        // 장소 그리드 리사이클러뷰 설정
        placeRecyclerView.layoutManager = GridLayoutManager(this, 2)
        placeRecyclerView.adapter = placeAdapter

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
                showSearchResults(keyword)
            }
        }
        
        // 탭 클릭 이벤트
        placeTab.setOnClickListener {
            if (currentSearchType != SearchType.PLACE) {
                setCurrentTab(SearchType.PLACE)
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchPlaces(query)
                }
            }
        }
        
        accountTab.setOnClickListener {
            if (currentSearchType != SearchType.ACCOUNT) {
                setCurrentTab(SearchType.ACCOUNT)
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchPosts(query)
                }
            }
        }
        
        // 초기 상태 설정
        showInitialState()
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
    
    // 검색 결과 화면 표시
    private fun showSearchResults(query: String) {
        // 초기 화면 요소 숨기기
        recentSearchesTitle.visibility = View.GONE
        popularSearchesTitle.visibility = View.GONE
        recentSearchesContainer.visibility = View.GONE
        
        // 인기 검색어 숨기기
        popularSearchItem1.visibility = View.GONE
        popularSearchItem2.visibility = View.GONE
        popularSearchItem3.visibility = View.GONE
        popularSearchItem4.visibility = View.GONE
        popularSearchItem5.visibility = View.GONE
        
        // 탭 레이아웃 보이기
        tabLayout.visibility = View.VISIBLE
        
        // 검색어 저장
        saveRecentSearch(query)
        
        // 현재 선택된 탭에 따라 다른 검색 실행
        if (currentSearchType == SearchType.PLACE) {
            searchPlaces(query)
        } else {
            searchPosts(query)
        }
    }

    // 검색 결과 화면 표시 (검색어 저장 없이)
    private fun showSearchResultsWithoutSaving(query: String) {
        // 초기 화면 요소 숨기기
        recentSearchesTitle.visibility = View.GONE
        popularSearchesTitle.visibility = View.GONE
        recentSearchesContainer.visibility = View.GONE
        
        // 인기 검색어 숨기기
        popularSearchItem1.visibility = View.GONE
        popularSearchItem2.visibility = View.GONE
        popularSearchItem3.visibility = View.GONE
        popularSearchItem4.visibility = View.GONE
        popularSearchItem5.visibility = View.GONE
        
        // 탭 레이아웃 보이기
        tabLayout.visibility = View.VISIBLE
        
        // 현재 선택된 탭에 따라 다른 검색 실행
        if (currentSearchType == SearchType.PLACE) {
            searchPlaces(query)
        } else {
            searchPosts(query)
        }
    }

    private fun searchPosts(query: String) {
        if (query.isEmpty()) {
            clearSearchResults()
            showInitialState()
            return
        }

        // 로딩 상태 표시
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        placeRecyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE
        
        // 약간의 지연 효과 (실제 API 호출 시뮬레이션)
        recyclerView.postDelayed({
            // 검색어가 비어있으면 검색 중단
            if (searchEditText.text.toString().isEmpty()) {
                clearSearchResults()
                showInitialState()
                return@postDelayed
            }

            // 검색어로 필터링
            val results = allPosts.filter { post ->
                post.userName.contains(query, ignoreCase = true) || 
                post.description.contains(query, ignoreCase = true)
            }
            
            // 결과 업데이트
            postAdapter.updateData(results)
            
            // 로딩 상태 해제
            progressBar.visibility = View.GONE
            
            // 탭에 따른 리사이클러뷰 표시
            setCurrentTab(SearchType.ACCOUNT)
            
            // 결과가 없으면 메시지 표시
            showNoResults(results.isEmpty())
        }, 500) // 0.5초 지연
    }
    
    private fun searchPlaces(query: String) {
        if (query.isEmpty()) {
            clearSearchResults()
            showInitialState()
            return
        }

        // 로딩 상태 표시
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        placeRecyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE
        
        // 약간의 지연 효과 (실제 API 호출 시뮬레이션)
        placeRecyclerView.postDelayed({
            // 검색어가 비어있으면 검색 중단
            if (searchEditText.text.toString().isEmpty()) {
                clearSearchResults()
                showInitialState()
                return@postDelayed
            }

            // 검색어로 필터링
            val results = if (query.equals("seoul", ignoreCase = true) || 
                              query.equals("서울", ignoreCase = true)) {
                // 서울 관련 검색어일 경우 커스텀 더미 이미지 사용
                listOf(
                    Place(
                        id = 101L,
                        name = "서울타워 N서울타워",
                        imageResource = R.drawable.search_place_dummy1
                    ),
                    Place(
                        id = 102L,
                        name = "서울숲",
                        imageResource = R.drawable.search_place_dummy2
                    ),
                    Place(
                        id = 103L,
                        name = "서울 여의도 한강공원",
                        imageResource = R.drawable.search_place_dummy3
                    ),
                    Place(
                        id = 104L,
                        name = "서울 경복궁",
                        imageResource = R.drawable.search_place_dummy4
                    )
                )
            } else {
                // 다른 검색어의 경우 기존 필터링 사용
                allPlaces.filter { place ->
                    place.name.contains(query, ignoreCase = true)
                }.map { place ->
                    // 평점과 거리 정보 제거
                    Place(
                        id = place.id,
                        name = place.name,
                        imageResource = place.imageResource
                    )
                }
            }
            
            // 결과 업데이트
            placeAdapter.updateData(results)
            
            // 로딩 상태 해제
            progressBar.visibility = View.GONE
            
            // 탭에 따른 리사이클러뷰 표시
            setCurrentTab(SearchType.PLACE)
            
            // 결과가 없으면 메시지 표시
            showNoResults(results.isEmpty())
        }, 500) // 0.5초 지연
    }
    
    private fun showNoResults(show: Boolean) {
        noResultsText.visibility = if (show) View.VISIBLE else View.GONE
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
                    showSearchResults(searchTerm)
                }
                
                // Margins 설정을 위해 LayoutParams 사용
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(
                    0,
                    0,
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    0
                )
                searchItemView.layoutParams = layoutParams
                
                horizontalLayout.addView(searchItemView)
            }
            
            // HorizontalScrollView에 추가
            horizontalScrollView.addView(horizontalLayout)
            
            // 최종적으로 컨테이너에 추가
            recentSearchesContainer.addView(horizontalScrollView)
        }
    }
} 