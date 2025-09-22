package com.example.travelonna.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.travelonna.R
import de.hdodenhof.circleimageview.CircleImageView

/**
 * 프로필 이미지 로딩을 위한 유틸리티 클래스
 */
object ImageUtils {
    
    /**
     * 프로필 이미지를 로드합니다. 
     * 이미지 URL이 null이거나 빈 문자열이면 기본 이미지(pf_image.png)를 사용합니다.
     * 
     * @param imageView 이미지를 표시할 ImageView 또는 CircleImageView
     * @param profileImageUrl 프로필 이미지 URL (null 또는 빈 문자열 가능)
     * @param placeholderRes 플레이스홀더 리소스 (기본값: pf_image)
     * @param errorRes 에러 시 표시할 리소스 (기본값: pf_image) 
     */
    fun loadProfileImage(
        imageView: ImageView,
        profileImageUrl: String?,
        placeholderRes: Int = R.drawable.pf_image,
        errorRes: Int = R.drawable.pf_image
    ) {
        val requestOptions = RequestOptions()
            .placeholder(placeholderRes)
            .error(errorRes)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
        
        if (profileImageUrl.isNullOrBlank()) {
            // 프로필 이미지 URL이 없으면 기본 이미지 사용
            Glide.with(imageView.context)
                .load(placeholderRes)
                .apply(requestOptions)
                .into(imageView)
        } else {
            // 프로필 이미지 URL이 있으면 해당 이미지 로드
            Glide.with(imageView.context)
                .load(profileImageUrl)
                .apply(requestOptions)
                .into(imageView)
        }
    }
    
    /**
     * CircleImageView 전용 프로필 이미지 로딩 메서드
     * 
     * @param circleImageView CircleImageView
     * @param profileImageUrl 프로필 이미지 URL
     */
    fun loadCircleProfileImage(
        circleImageView: CircleImageView,
        profileImageUrl: String?
    ) {
        loadProfileImage(circleImageView, profileImageUrl)
    }
    
    /**
     * 현재 사용자의 프로필 이미지를 로드하는 헬퍼 메서드
     * 추후 사용자 프로필 API 연동 시 사용
     */
    fun loadCurrentUserProfileImage(imageView: ImageView) {
        // TODO: 현재 로그인한 사용자의 프로필 이미지 URL을 가져와서 로드
        // 현재는 기본 이미지만 사용
        loadProfileImage(imageView, null)
    }
} 