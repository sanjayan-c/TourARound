import android.os.Parcel
import android.os.Parcelable

data class LocationData (
    val id: String,
    val locationName: String,
    val locationDesc: String,
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    // Add a no-argument constructor
    constructor() : this("", "", "", 0.0, 0.0)

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(locationName)
        parcel.writeString(locationDesc)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocationData> {
        override fun createFromParcel(parcel: Parcel): LocationData {
            return LocationData(parcel)
        }

        override fun newArray(size: Int): Array<LocationData?> {
            return arrayOfNulls(size)
        }
    }
}
