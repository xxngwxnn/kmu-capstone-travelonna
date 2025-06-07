package com.example.travelonna.util

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.locationtech.proj4j.*

object CoordinateConverter {
    private val crsFactory = CRSFactory()
    private val ctFactory = CoordinateTransformFactory()
    
    // EPSG:5179 (Korea 2000 / Central Belt)
    private val sourceCRS = crsFactory.createFromParameters(
        "EPSG:5179",
        "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=GRS80 +units=m +no_defs"
    )
    
    // EPSG:4326 (WGS84)
    private val targetCRS = crsFactory.createFromName("EPSG:4326")
    
    private val transform = ctFactory.createTransform(sourceCRS, targetCRS)
    
    fun convertToLatLng(x: Double, y: Double): LatLng {
        try {
            val srcPt = ProjCoordinate(x, y)
            val dstPt = ProjCoordinate()
            
            transform.transform(srcPt, dstPt)
            
            // 좌표 변환 결과 로깅
            Log.d("CoordinateConverter", "Converting ($x, $y) to (${dstPt.y}, ${dstPt.x})")
            
            return LatLng(dstPt.y, dstPt.x)
        } catch (e: Exception) {
            Log.e("CoordinateConverter", "Error converting coordinates ($x, $y)", e)
            throw e
        }
    }
    
    fun convertCoordinatesList(coordinates: List<List<Double>>): List<LatLng> {
        return coordinates.map { coord ->
            convertToLatLng(coord[0], coord[1])
        }
    }
} 