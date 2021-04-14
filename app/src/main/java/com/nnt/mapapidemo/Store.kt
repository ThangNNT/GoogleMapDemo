package com.nnt.mapapidemo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Store(val id: Int,
                 val name: String,
                 val latitude: Double,
                 val longitude: Double,
                 val url: String): Parcelable{
    companion object{
        fun getStoreExampleList(): List<Store>{
            val stores = LinkedList<Store>()
            stores.add(Store(1, "Cửa hàng Thắng Lợi", 21.0115134095216,105.80817718058825,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(2, "Cửa hàng Chiến Thắng", 21.0021192396409,105.80813929438591,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(3, "Xe máy tốt", 21.007194180505678,105.80241378396748,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(4, "Thế giới xe máy", 21.007874623867476,105.81352684646845,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(5, "Xe máy Hoàng Hải", 21.005175054546015,105.81262059509756,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(6, "Xe của Thắng", 21.009830177026473,105.80555029213428,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(7, "Xe máy Hùng Cường", 21.004364704728587,105.80597642809153,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            stores.add(Store(8, "Xe máy Hùng Hải", 21.009943165150077,105.81068940460683,"https://motorstore.vn/wp-content/uploads/2018/12/motorbiker-helmets-hanoi.jpg"))
            return stores
        }
    }
}