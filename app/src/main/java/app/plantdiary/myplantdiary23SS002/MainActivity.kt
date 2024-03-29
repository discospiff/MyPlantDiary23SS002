package app.plantdiary.myplantdiary23SS002

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.inputmethodservice.Keyboard
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.plantdiary.myplantdiary23SS002.ui.theme.MyPlantDiaryTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.plantdiary.myplantdiary23SS002.dto.Photo
import app.plantdiary.myplantdiary23SS002.dto.Plant
import app.plantdiary.myplantdiary23SS002.dto.Specimen
import app.plantdiary.myplantdiary23SS002.dto.User
import coil.compose.AsyncImage
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {

    private var uri: Uri? = null
    private lateinit var currentImagePath: String
    private var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var selectedPlant: Plant? = null
    var inPlantName : String = ""

    // get our ViewModel from Koin
    private val viewModel: MainViewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel.fetchPlants()
            firebaseUser?.let {
                var user = User (it.uid, it.displayName)
                viewModel.user = user
                viewModel.listenToSpecimens()
            }
            val plants by viewModel.plants.observeAsState(initial = emptyList())
            MyPlantDiaryTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpecimenFacts(plants)
                }
                var foo = plants
                var i = 1 + 1
            }
        }
    }


    @Composable
    fun SpecimenFacts(plants : List<Plant> = ArrayList<Plant>()) {

        var inLocation by remember(viewModel.selectedSpecimen.specimenId) { mutableStateOf(viewModel.selectedSpecimen.location) }
        var inDescription by remember(viewModel.selectedSpecimen.specimenId) { mutableStateOf(viewModel.selectedSpecimen.description) }
        var inDatePlanted by remember(viewModel.selectedSpecimen.specimenId) { mutableStateOf(viewModel.selectedSpecimen.datePlanted) }
        val context = LocalContext.current
        Column {
            val specimens by viewModel.specimens.observeAsState(initial = emptyList())

            SpecimenSpinner(specimens = specimens)
            TextFieldWithDropDownUsage(dataIn = plants, "Plant Name")
            OutlinedTextField(
                value = inLocation,
                onValueChange = { inLocation = it },
                label = { Text(stringResource(R.string.location)) }
            )
            OutlinedTextField(
                value = inDescription,
                onValueChange = { inDescription = it },
                label = { Text(stringResource(R.string.description)) }
            )
            OutlinedTextField(
                value = inDatePlanted,
                onValueChange = { inDatePlanted = it },
                label = { Text(stringResource(R.string.datePlanted)) }
            )
            Row {
                Button(
                    onClick = {
                        viewModel.selectedSpecimen.apply {
                            plantName = inPlantName
                            plantId = selectedPlant?.let {
                                it.id
                            } ?: -1
                            location = inLocation
                            description = inDescription
                            datePlanted = inDatePlanted
                        }
                        viewModel.save()
                        Toast.makeText(
                            context,
                            "$inPlantName $inLocation $inDescription $inDatePlanted",
                            Toast.LENGTH_LONG
                        ).show()
                    },

                    ) { Text(text = "Save") }
                Button(
                    onClick = {
                        signIn()
                    }
                ) {
                    Text(text = "Logon")
                }
                Button(
                    onClick = {
                        takePhoto()
                    }
                ) {
                    Text(text = "Photo")
                }
            }
            Events()
        }
    }

    private @Composable
    fun Events() {
        val photos by viewModel.eventPhotos.observeAsState(initial = emptyList())
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.fillMaxHeight()) {
            items (
                items = photos,
                itemContent = {EventListItem(photo = it)}
            )
        }
    }

    private fun takePhoto() {
        if (hasCameraPermission() == PERMISSION_GRANTED && hasExternalStoragePermission() == PERMISSION_GRANTED) {
            invokeCamera()
        } else {
            // request permissions
            requestMultiplePermissionsLauncher.launch(arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ))
        }
    }

    private fun invokeCamera() {
        var file = createImageFile()
        uri =FileProvider.getUriForFile(this, "app.plantdiary.myplantdiary23SS002.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("Specimen_$timestamp",
            ".jpg",
            imageDirectory).apply {
                currentImagePath = absolutePath
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        success ->
        if (success) {
            Log.i("MainActivity", "Image Location $uri")
            var strUri = uri.toString()
            val photo = Photo(localUri = strUri)
            viewModel.photos.add(photo)
        } else {
            Log.e("MainActivity", "IMage not saved $uri")
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        resultsMap ->
        var permissionGranted = false

        resultsMap.forEach { permission, isGranted ->
            if (!isGranted) {
                return@forEach
            }
            permissionGranted  = isGranted
        }

        if (permissionGranted) {
            invokeCamera()
        } else {
            Toast.makeText(this, "I can't take a photo if you don't give me permission", Toast.LENGTH_LONG).show()
        }
    }

    fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    fun hasExternalStoragePermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun signIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) {
        signInResult(it)
    }

    /**
     * Handle logged in user.
     */
    private fun signInResult(result: FirebaseAuthUIAuthenticationResult?) {
        result?.let{
                result ->
                val response = result.idpResponse

                if (result.resultCode == RESULT_OK) {
                    firebaseUser = FirebaseAuth.getInstance().currentUser
                    firebaseUser?.let {
                        val user = User(it.uid, it.displayName)
                        viewModel.user = user
                        viewModel.saveUser()
                        viewModel.listenToSpecimens()
                    }
                } else {
                    Log.e("MainActivity.kt", "Error logging in: " + response?.error?.errorCode)
                }
        }
    }

    @Composable
    fun SpecimenSpinner(specimens : List<Specimen>) {
        var specimenText by remember {mutableStateOf("Specimen Collection")}
        var expanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .padding(24.dp)
                    .clickable { expanded = !expanded }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text (text = specimenText, fontSize = 18.sp, modifier = Modifier.padding(end=8.dp))
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Drop down arrow")
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    specimens.forEach {
                        specimen -> DropdownMenuItem(onClick = { expanded = false
                        if (specimen.plantName == viewModel.NEW_SPECIMEN) {
                            specimenText = ""
                            specimen.plantName = ""
                        } else {
                            specimenText = specimen.toString()
                        }
                        viewModel.selectedSpecimen = specimen
                        viewModel.fetchPhotos()
                    }) {
                       Text(text = specimen.toString())
                    }
                    }
                }
            }
        }
    }

    @Composable
    fun TextFieldWithDropDownUsage(dataIn: List<Plant>, label: String = "", take: Int = 3) {
        val dropDownOptions =remember() {mutableStateOf(listOf<Plant>())}
        val textFieldValue = remember(viewModel.selectedSpecimen.specimenId) {mutableStateOf(TextFieldValue(viewModel.selectedSpecimen.plantName))}
        val dropDownExpanded = remember() {mutableStateOf(false)}
        fun onDropdownDismissRequest() {
            dropDownExpanded.value = false
        }

        fun onValueChanged(value: TextFieldValue) {
            inPlantName = value.text
            dropDownExpanded.value = true
            textFieldValue.value = value
            dropDownOptions.value = dataIn.filter {
                it.toString().startsWith(value.text) && it.toString() != value.text
            }.take(take)
        }

        TextFieldWithDropdown(
            modifier = Modifier.fillMaxWidth(),
            value = textFieldValue.value,
            setValue = ::onValueChanged,
            onDismissRequest = ::onDropdownDismissRequest,
            dropDownExpanded = dropDownExpanded.value,
            list = dropDownOptions.value,
            label = label
        )

    }


    @Composable
    fun TextFieldWithDropdown(
        modifier: Modifier = Modifier,
        value: TextFieldValue,
        setValue: (TextFieldValue) -> Unit,
        onDismissRequest: () -> Unit,
        dropDownExpanded: Boolean,
        list: List<Plant>,
        label: String = "Plant Name"
    ) {
        Box(modifier) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused)
                            onDismissRequest()
                    },
                value = value,
                onValueChange = setValue,
                label = { Text(label) },
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
            DropdownMenu(
                expanded = dropDownExpanded,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                onDismissRequest = onDismissRequest
            ) {
                list.forEach { plant ->
                    DropdownMenuItem(onClick = {
                        setValue(
                            TextFieldValue(
                                plant.toString(),
                                TextRange(plant.toString().length)
                            )
                        )
                        selectedPlant = plant
                    }) {
                        Text(text = plant.toString())
                    }
                }
            }
        }
    }

    @Composable
    fun EventListItem(photo: Photo) {
        var inAltText by remember(photo.id) {mutableStateOf(photo.altText)}
        Row {
            Column(Modifier.weight(2f)) {
                AsyncImage(model = photo.localUri, contentDescription = photo.altText, Modifier.width(64.dp).height(64.dp))
            }
            Column(Modifier.weight(4f)) {
                Text(text = photo.id, style=typography.h6)
                Text(text = photo.dateTaken.toString(), style =typography.caption)
                OutlinedTextField(
                    value = inAltText,
                    onValueChange = {inAltText = it},
                    label = {Text("Alt Text")},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                Button (
                    onClick = {
                        photo.altText = inAltText
                        save(photo)
                    }) {
                    Icon (
                        imageVector = Icons.Filled.Check,
                        contentDescription =  "Save",
                        modifier = Modifier.padding(end=8.dp)
                            )
                }
            }
        }
    }

    private fun save(photo: Photo) {
        TODO("Not yet implemented")
    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        MyPlantDiaryTheme {
            SpecimenFacts(plants = ArrayList<Plant>())
        }
    }
}