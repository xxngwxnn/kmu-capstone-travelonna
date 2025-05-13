package com.example.travelonna.data

import com.example.travelonna.api.Station

/**
 * 주요 도시별 역 정보를 포함하는 정적 데이터 클래스
 */
object StationData {
    
    // 지역별 역 정보
    val stationsByRegion = mapOf(
        "서울특별시" to listOf(
            Station("1", "서울역", "KTX/SRT", "서울특별시 용산구"),
            Station("2", "용산역", "KTX/ITX", "서울특별시 용산구"),
            Station("3", "청량리역", "KTX/ITX", "서울특별시 동대문구"),
            Station("7", "광명역", "KTX/SRT", "경기도 광명시"),
            Station("9", "수서역", "SRT", "서울특별시 강남구")
        ),
        "부산광역시" to listOf(
            Station("10", "부산역", "KTX/SRT", "부산광역시 동구"),
            Station("13", "구포역", "일반열차", "부산광역시 북구"),
            Station("14", "부전역", "일반열차", "부산광역시 진구"),
            Station("16", "해운대역", "일반열차", "부산광역시 해운대구")
        ),
        "대구광역시" to listOf(
            Station("17", "동대구역", "KTX/SRT", "대구광역시 동구"),
            Station("18", "대구역", "일반열차", "대구광역시 중구"),
            Station("21", "서대구역", "일반열차", "대구광역시 서구"),
            Station("22", "경산역", "일반열차", "경상북도 경산시"),
            Station("23", "칠곡역", "일반열차", "경상북도 칠곡군")
        ),
        "인천광역시" to listOf(
            Station("25", "인천역", "일반열차", "인천광역시 중구"),
            Station("29", "부평역", "일반열차", "인천광역시 부평구"),
            Station("30", "주안역", "일반열차", "인천광역시 미추홀구")
        ),
        "광주광역시" to listOf(
            Station("31", "광주송정역", "KTX", "광주광역시 광산구"),
            Station("32", "광주역", "일반열차", "광주광역시 북구"),
            Station("35", "남광주역", "일반열차", "광주광역시 동구"),
            Station("36", "화순역", "일반열차", "전라남도 화순군")
        ),
        "대전광역시" to listOf(
            Station("37", "대전역", "KTX", "대전광역시 동구"),
            Station("38", "서대전역", "일반열차", "대전광역시 중구"),
            Station("40", "신탄진역", "일반열차", "대전광역시 대덕구"),
            Station("41", "회덕역", "일반열차", "대전광역시 유성구")
        ),
        "울산광역시" to listOf(
            Station("42", "울산역", "KTX", "울산광역시 울주군"),
            Station("44", "태화강역", "일반열차", "울산광역시 중구"),
            Station("45", "통도사역", "일반열차", "울산광역시 울주군"),
            Station("46", "호계역", "일반열차", "울산광역시 북구")
        ),
        "경기도" to listOf(
            Station("47", "수원역", "KTX/일반열차", "경기도 수원시"),
            Station("50", "안양역", "일반열차", "경기도 안양시"),
            Station("51", "의정부역", "일반열차", "경기도 의정부시"),
            Station("52", "평택역", "KTX/일반열차", "경기도 평택시"),
            Station("53", "동탄역", "KTX/SRT", "경기도 화성시"),
            Station("54", "지행역", "일반열차", "경기도 의정부시"),
            Station("55", "오산역", "일반열차", "경기도 오산시"),
            Station("56", "성남역", "일반열차", "경기도 성남시"),
            Station("57", "부천역", "일반열차", "경기도 부천시"),
            Station("58", "일산역", "일반열차", "경기도 고양시")
        ),
        "강원도" to listOf(
            Station("59", "춘천역", "ITX", "강원도 춘천시"),
            Station("60", "원주역", "KTX/ITX", "강원도 원주시"),
            Station("61", "강릉역", "KTX", "강원도 강릉시"),
            Station("65", "정동진역", "일반열차", "강원도 강릉시"),
            Station("66", "평창역", "일반열차", "강원도 평창군"),
            Station("67", "횡성역", "일반열차", "강원도 횡성군"),
            Station("68", "삼척역", "일반열차", "강원도 삼척시")
        ),
        "충청북도" to listOf(
            Station("69", "청주역", "일반열차", "충청북도 청주시"),
            Station("70", "충주역", "일반열차", "충청북도 충주시"),
            Station("72", "오송역", "KTX", "충청북도 청주시"),
            Station("73", "제천역", "일반열차", "충청북도 제천시"),
            Station("74", "영동역", "일반열차", "충청북도 영동군")
        ),
        "충청남도" to listOf(
            Station("75", "천안역", "KTX/일반열차", "충청남도 천안시"),
            Station("76", "아산역", "KTX", "충청남도 아산시"),
            Station("77", "논산역", "일반열차", "충청남도 논산시"),
            Station("79", "공주역", "KTX", "충청남도 공주시"),
            Station("80", "홍성역", "일반열차", "충청남도 홍성군"),
            Station("81", "온양온천역", "일반열차", "충청남도 아산시"),
            Station("82", "예산역", "일반열차", "충청남도 예산군")
        ),
        "전라북도" to listOf(
            Station("84", "전주역", "일반열차", "전라북도 전주시"),
            Station("85", "익산역", "KTX", "전라북도 익산시"),
            Station("86", "정읍역", "KTX", "전라북도 정읍시"),
            Station("88", "군산역", "일반열차", "전라북도 군산시"),
            Station("89", "남원역", "일반열차", "전라북도 남원시"),
            Station("90", "김제역", "일반열차", "전라북도 김제시")
        ),
        "전라남도" to listOf(
            Station("91", "목포역", "KTX", "전라남도 목포시"),
            Station("92", "여수엑스포역", "KTX", "전라남도 여수시"),
            Station("93", "순천역", "일반열차", "전라남도 순천시"),
            Station("95", "광양역", "일반열차", "전라남도 광양시"),
            Station("96", "나주역", "KTX", "전라남도 나주시"),
            Station("98", "구례구역", "일반열차", "전라남도 구례군"),
            Station("99", "보성역", "일반열차", "전라남도 보성군")
        ),
        "경상북도" to listOf(
            Station("100", "포항역", "KTX", "경상북도 포항시"),
            Station("101", "경주역", "KTX", "경상북도 경주시"),
            Station("102", "안동역", "일반열차", "경상북도 안동시"),
            Station("103", "구미역", "KTX", "경상북도 구미시"),
            Station("105", "영주역", "일반열차", "경상북도 영주시"),
            Station("106", "김천역", "일반열차", "경상북도 김천시"),
            Station("107", "김천구미역", "KTX", "경상북도 김천시"),
            Station("108", "신경주역", "KTX", "경상북도 경주시"),
            Station("109", "영천역", "일반열차", "경상북도 영천시")
        ),
        "경상남도" to listOf(
            Station("110", "창원역", "일반열차", "경상남도 창원시"),
            Station("111", "마산역", "일반열차", "경상남도 창원시"),
            Station("112", "진주역", "KTX", "경상남도 진주시"),
            Station("114", "밀양역", "KTX", "경상남도 밀양시"),
            Station("115", "진해역", "일반열차", "경상남도 창원시")
        )
    )
    
    // 모든 지역 목록 반환
    fun getAllRegions(): List<String> {
        return stationsByRegion.keys.toList()
    }
    
    // 역 검색 함수 (키워드로 검색) - 정확한 지역 이름만 검색
    fun searchStations(keyword: String): List<Station> {
        if (keyword.isBlank()) return emptyList()
        
        val lowerKeyword = keyword.lowercase()
        val results = mutableListOf<Station>()
        
        // 정확한 지역명 비교를 위한 검색
        stationsByRegion.forEach { (region, stations) ->
            // 지역명이 정확히 포함된 경우에만 역 포함
            if (region.lowercase().contains(lowerKeyword)) {
                stations.forEach { station ->
                    // 역 이름이 키워드를 포함하는 경우에만 추가
                    if (station.name.lowercase().contains(lowerKeyword) ||
                        station.type.lowercase().contains(lowerKeyword)) {
                        results.add(station)
                    }
                }
            }
        }
        
        return results
    }
    
    // 역 검색 함수 (역 이름으로만 검색)
    fun searchStationsByName(keyword: String): List<Station> {
        if (keyword.isBlank()) return emptyList()
        
        val lowerKeyword = keyword.lowercase()
        val results = mutableListOf<Station>()
        
        stationsByRegion.values.forEach { stations ->
            stations.forEach { station ->
                if (station.name.lowercase().contains(lowerKeyword)) {
                    results.add(station)
                }
            }
        }
        
        return results
    }
    
    // 지역별 역 검색
    fun getStationsByRegion(region: String): List<Station> {
        return stationsByRegion[region] ?: emptyList()
    }
} 