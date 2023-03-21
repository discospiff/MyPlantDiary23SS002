package app.plantdiary.myplantdiary23SS002.dto

data class Plant(var genus : String, var species : String, var common : String = "", var id : Int = 0) {
    override fun toString() = common
}

