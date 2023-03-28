package app.plantdiary.myplantdiary23SS002

import android.inputmethodservice.Keyboard
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import app.plantdiary.myplantdiary23SS002.dto.Plant
import app.plantdiary.myplantdiary23SS002.dto.Specimen

class MainActivity : ComponentActivity() {

    private var selectedPlant: Plant? = null
    var inPlantName : String = ""

    // get our ViewModel from Koin
    private val viewModel: MainViewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel.fetchPlants()
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


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        MyPlantDiaryTheme {
            SpecimenFacts(plants = ArrayList<Plant>())
        }
    }
}