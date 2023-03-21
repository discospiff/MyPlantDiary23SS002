package app.plantdiary.myplantdiary23SS002

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.plantdiary.myplantdiary23SS002.service.IPlantService
import app.plantdiary.myplantdiary23SS002.service.PlantService
import androidx.lifecycle.viewModelScope
import app.plantdiary.myplantdiary23SS002.dto.Plant
import app.plantdiary.myplantdiary23SS002.dto.Specimen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.launch

class MainViewModel(var plantService : IPlantService = PlantService()) : ViewModel() {

    var plants : MutableLiveData<List<Plant>> = MutableLiveData<List<Plant>>()

    private lateinit var firestore : FirebaseFirestore

    init {
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()
    }

    fun fetchPlants() {
        viewModelScope.launch {
            var innerPlants = plantService.fetchPlants()
            plants.postValue(innerPlants)
        }
    }

    fun save(specimen: Specimen) {
        val document =
            if (specimen.specimenId == null || specimen.specimenId.isEmpty()) {
                firestore.collection("specimens").document()
            } else {
                firestore.collection("specimens").document(specimen.specimenId)
            }
        specimen.specimenId = document.id
        val handle = document.set(specimen)
        handle.addOnSuccessListener { Log.d("Firebase", "Document saved") }
        handle.addOnFailureListener { Log.e("FIrebase", "Save failed $it") }
    }

}
