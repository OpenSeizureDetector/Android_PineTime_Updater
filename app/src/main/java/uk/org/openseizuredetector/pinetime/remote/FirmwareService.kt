package uk.org.openseizuredetector.pinetime.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import uk.org.openseizuredetector.pinetime.model.FirmwareIndex

interface FirmwareService {

    @GET("static/pineTimeSdFw/index.json")
    suspend fun getIndex(): FirmwareIndex

    @GET("static/pineTimeSdFw/{fileName}")
    suspend fun downloadFirmware(@Path("fileName") fileName: String): Response<ResponseBody>
}
