package app.plantdiary.myplantdiary23SS002

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.plantdiary.myplantdiary23SS002.service.IPlantService
import app.plantdiary.myplantdiary23SS002.service.PlantService
import androidx.lifecycle.viewModelScope
import app.plantdiary.myplantdiary23SS002.dto.Plant
import app.plantdiary.myplantdiary23SS002.dto.Specimen
import app.plantdiary.myplantdiary23SS002.dto.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.launch

class MainViewModel(var plantService : IPlantService = PlantService()) : ViewModel() {

    var user: User? = null
    internal val NEW_SPECIMEN = "NEW SPECIMEN"
    var selectedSpecimen by mutableStateOf(Specimen())
    var plants : MutableLiveData<List<Plant>> = MutableLiveData<List<Plant>>()
    var specimens : MutableLiveData<List<Specimen>> = MutableLiveData<List<Specimen>>()

    private lateinit var firestore : FirebaseFirestore

    init {
        firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().build()
    }

    fun listenToSpecimens() {
        user?.let {
            user ->
            firestore.collection("users").document(user.uid).collection("specimens").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // something went wrong.
                    Log.w(
                        "MainViewModel.listenToSpecimens()",
                        "Error occurred retreiving specimens ${error.message}"
                    )
                    return@addSnapshotListener
                }
                // if we're here, we have specimens!
                snapshot?.let {
                    val allSpecimens = ArrayList<Specimen>()
                    allSpecimens.add(Specimen(plantName = NEW_SPECIMEN))
                    val documents = snapshot.documents
                    documents.forEach { specimenDocument ->
                        val specimen = specimenDocument.toObject(Specimen::class.java)
                        specimen?.let {
                            allSpecimens.add(it)
                        }
                    }
                    specimens.value = allSpecimens
                }
            }
        }
    }

    fun fetchPlants() {
        viewModelScope.launch {
            var innerPlants = plantService.fetchPlants()
            plants.postValue(innerPlants)
        }
    }

    fun save() {
        user?.let {
            user ->
            val document =
                if (selectedSpecimen.specimenId == null || selectedSpecimen.specimenId.isEmpty()) {
                    firestore.collection("users").document(user.uid).collection("specimens").document()
                } else {
                    firestore.collection("users").document(user.uid).collection("specimens").document(selectedSpecimen.specimenId)
                }
            selectedSpecimen.specimenId = document.id
            val handle = document.set(selectedSpecimen)
            handle.addOnSuccessListener { Log.d("Firebase", "Document saved") }
            handle.addOnFailureListener { Log.e("FIrebase", "Save failed $it") }
        }
    }

    fun saveUser () {
        user?.let {
            user ->
            val handle = firestore.collection("users").document(user.uid).set(user)
            handle.addOnSuccessListener { Log.d("Firebase", "Document Saved") }
            handle.addOnFailureListener { Log.e("Firebase", "Save failed $it") }
        }
    }

}
