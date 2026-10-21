package ph.org.daluyan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import ph.org.daluyan.data.*
import ph.org.daluyan.domain.*
import ph.org.daluyan.ui.MapPanel
import java.io.File
import java.util.Locale

private val Green = Color(0xFF17684E)
private val Dark = Color(0xFF20332D)
private val OffWhite = Color(0xFFF3F7F4)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity: ComponentActivity() {
    private val store by lazy { ReportDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext)
        setContent {
            val scope = rememberCoroutineScope()
            val records = remember { mutableStateListOf<SiteReport>() }
            val evidence = remember { OfficialData.read(applicationContext) }
            var selected by remember { mutableStateOf<Pair<Double, Double>?>(null) }
            var page by remember { mutableIntStateOf(0) }
            var formOpen by remember { mutableStateOf(false) }
            var notice by remember { mutableStateOf<String?>(null) }
            var exportContent by remember { mutableStateOf("") }
            var confirmDelete by remember { mutableStateOf<SiteReport?>(null) }
            var photoUri by remember { mutableStateOf<Uri?>(null) }
            var proposedPhoto by remember { mutableStateOf<Uri?>(null) }
            var requestedGps by remember { mutableStateOf(false) }
            val toast = remember { SnackbarHostState() }

            suspend fun refresh() {
                val loaded = withContext(Dispatchers.IO) { store.all() }
                records.clear()
                records.addAll(loaded)
            }
            LaunchedEffect(Unit) {
                try { refresh() } catch (e: Exception) { notice = "Could not load saved reports." }
            }
            LaunchedEffect(notice) {
                notice?.let { toast.showSnackbar(it); notice = null }
            }
            val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                if (uri != null) scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            contentResolver.openOutputStream(uri)?.use { it.write(exportContent.toByteArray(Charsets.UTF_8)) }
                                ?: error("Cannot create document")
                        }
                        notice = "CSV exported."
                    } catch (e: Exception) { notice = "Export failed." }
                }
            }
            val photoCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { result ->
                if (result) photoUri = proposedPhoto
                else {
                    proposedPhoto?.let { runCatching { contentResolver.delete(it, null, null) } }
                    proposedPhoto = null
                }
            }
            val capture = {
                try {
                    val dir = File(filesDir, "evidence").also { it.mkdirs() }
                    val file = File.createTempFile("site_", ".jpg", dir)
                    proposedPhoto = FileProvider.getUriForFile(this, packageName + ".fileprovider", file)
                    photoCapture.launch(proposedPhoto!!)
                } catch (e: Exception) { notice = "Camera is unavailable." }
            }
            val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    locate { lat, lon, problem ->
                        if (lat != null && lon != null) selected = lat to lon
                        notice = problem ?: "Location selected. Check GPS accuracy."
                    }
                } else notice = "Permission denied. Tap map or type coordinates instead."
                requestedGps = false
            }
            val locateRequest = {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locate { lat, lon, problem ->
                        if (lat != null && lon != null) selected = lat to lon
                        notice = problem ?: "Location selected. Check GPS accuracy."
                    }
                } else if (!requestedGps) {
                    requestedGps = true
                    locPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            val clearPhoto = {
                photoUri?.let { runCatching { contentResolver.delete(it, null, null) } }
                if (proposedPhoto != photoUri) proposedPhoto?.let { runCatching { contentResolver.delete(it, null, null) } }
                photoUri = null
                proposedPhoto = null
            }
            val theme = lightColorScheme(primary = Green, secondary = Green, background = OffWhite,
                surface = Color.White, onSurface = Dark, onBackground = Dark)
            MaterialTheme(colorScheme = theme) {
                Scaffold(
                    containerColor = OffWhite,
                    topBar = {
                        Surface(color = OffWhite) {
                            Column(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=16.dp)) {
                                Text("DALUYAN", color=Green, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.ExtraBold)
                                Text("Evidence into environmental action", style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(containerColor=Color.White) {
                            listOf(Triple("Explore",Icons.Default.Map,0),
                                Triple("Sites",Icons.Default.Place,1),Triple("Evidence",Icons.Default.Assessment,2))
                                .forEach { (title,icon,number) ->
                                    NavigationBarItem(selected=page==number,onClick={page=number},
                                        icon={Icon(icon,contentDescription=title)},label={Text(title)})
                                }
                        }
                    },
                    snackbarHost = { SnackbarHost(toast) }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                        .padding(horizontal=18.dp, vertical=10.dp)) {
                        when(page) {
                            0 -> {
                                Text("Explore Calapan",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                                Text("Real maps · community observations",style=MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(16.dp))
                                Tile {
                                    Row(verticalAlignment=Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Community map",fontWeight=FontWeight.SemiBold)
                                            Text("Tap to select a point",style=MaterialTheme.typography.bodySmall)
                                        }
                                        Text(records.size.toString()+" sites", color=Green)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    MapPanel(records.toList(),selected) { lat,lon -> selected=lat to lon }
                                    Spacer(Modifier.height(8.dp))
                                    Text("OpenStreetMap-derived tiles need internet. Records remain on your phone.",style=MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(onClick=locateRequest,modifier=Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.MyLocation,null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Use my GPS location")
                                    }
                                    selected?.let { (lat,lon) ->
                                        Text("Selected: "+String.format(Locale.US,"%.5f, %.5f",lat,lon),color=Green,
                                            style=MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Tile {
                                    Text("Document an environmental site",fontWeight=FontWeight.Bold,
                                        style=MaterialTheme.typography.titleMedium)
                                    Text("Create a firsthand, unverified litter report with an explainable planning priority.",
                                        style=MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick={clearPhoto();formOpen=true},modifier=Modifier.fillMaxWidth()) { Text("Record observation") }
                                    Text("Do not enter unsafe areas or private property.",style=MaterialTheme.typography.labelSmall)
                                }
                            }
                            1 -> {
                                Text("Priority sites",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                                Text("Planning scores, not official risk predictions.",style=MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(12.dp))
                                if (records.isEmpty()) {
                                    Tile {
                                        Text("No reports yet",fontWeight=FontWeight.Bold)
                                        Text("Record a genuine field observation to get started.")
                                        Button(onClick={formOpen=true}) { Text("Record site") }
                                    }
                                } else {
                                    OutlinedButton(onClick={
                                        exportContent=CsvExport.generate(records.toList())
                                        csv.launch("daluyan-sites.csv")
                                    },modifier=Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.FileDownload,null)
                                        Spacer(Modifier.width(8.dp));Text("Export CSV")
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    records.sortedWith(compareBy<SiteReport> {it.access==SiteAccess.UNSAFE}
                                        .thenByDescending {PriorityEngine.score(it).total})
                                        .forEach { record ->
                                            SiteTile(record,onDelete={ confirmDelete=record })
                                            Spacer(Modifier.height(10.dp))
                                        }
                                    Text("Restricted/unsafe sites are listed last; never enter them.",
                                        style=MaterialTheme.typography.labelSmall)
                                }
                            }
                            else -> {
                                Text("Official evidence",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                                Text("2025 published water quality · not live pollution readings.",
                                    style=MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(12.dp))
                                Tile {
                                    Text("DENR-EMB MIMAROPA",color=Green,fontWeight=FontWeight.Bold)
                                    Text("Fecal coliform monitoring (MPN/100mL). Not a measure of plastic litter.",
                                        style=MaterialTheme.typography.bodySmall)
                                    TextButton(onClick={
                                        startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(OfficialData.SOURCE_URL)))
                                    }) {
                                        Icon(Icons.Default.OpenInNew,null)
                                        Text("Published source")
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                evidence.forEach { result ->
                                    Tile {
                                        Row(verticalAlignment=Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(result.station,fontWeight=FontWeight.Bold)
                                                Text(result.waterbody+" · Class "+result.classification,
                                                    style=MaterialTheme.typography.bodySmall)
                                            }
                                            Text(result.value.toString(),color=if(result.value>result.guideline) Color(0xFF9C4938) else Green,
                                                fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                                        }
                                        Text("Guideline: "+result.guideline+" "+result.unit+" · "+result.period,
                                            style=MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text("Station coordinates have not been confirmed, so no official station markers are invented.",
                                    style=MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                if (formOpen) {
                    ModalBottomSheet(onDismissRequest={clearPhoto();formOpen=false}) {
                        ReportForm(initial=selected,photo=photoUri,onPhoto=capture,onCancel={clearPhoto();formOpen=false}) { report,onResult ->
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { store.insert(report) }
                                    refresh()
                                    onResult(true)
                                    photoUri=null
                                    proposedPhoto=null
                                    formOpen=false
                                    page=1
                                    notice="Observation stored on this device."
                                } catch (e: Exception) {
                                    onResult(false)
                                    notice="Could not save the observation."
                                }
                            }
                        }
                    }
                }
                confirmDelete?.let { record ->
                    AlertDialog(onDismissRequest={confirmDelete=null},
                        title={Text("Delete report?")},
                        text={Text("This removes a local observation. Exported CSVs are unaffected.")},
                        confirmButton={TextButton(onClick={
                            scope.launch {
                                try {
                                    val ok=withContext(Dispatchers.IO) {store.delete(record.id)}
                                    if (ok) {
                                        record.photoUri?.let { runCatching { contentResolver.delete(Uri.parse(it),null,null) } }
                                        refresh()
                                        notice="Observation deleted."
                                    }
                                } catch (e: Exception) { notice="Deletion failed." }
                                confirmDelete=null
                            }
                        }){Text("Delete")}},
                        dismissButton={TextButton(onClick={confirmDelete=null}){Text("Cancel")}})
                }
            }
        }
    }

    private fun locate(callback:(Double?,Double?,String?)->Unit) {
        try {
            val manager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider=listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER)
                .firstOrNull {manager.isProviderEnabled(it)}
            if (provider==null) {
                callback(null,null,"Enable location or enter coordinates.")
                return
            }
            if (Build.VERSION.SDK_INT>=30) {
                manager.getCurrentLocation(provider,null,ContextCompat.getMainExecutor(this)) { result ->
                    callback(result?.latitude,result?.longitude,
                        if(result==null) "No GPS fix. Choose a map point." else null)
                }
            } else {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider,object:LocationListener {
                    override fun onLocationChanged(location:Location) {
                        callback(location.latitude,location.longitude,null)
                    }
                },null)
            }
        } catch(e:SecurityException) {
            callback(null,null,"Location permission is unavailable.")
        } catch(e:Exception) {
            callback(null,null,"Could not retrieve location.")
        }
    }
}

@Composable
private fun Tile(content:@Composable ColumnScope.()->Unit) {
    Surface(color=Color.White,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp),content=content)
    }
}

@Composable
private fun SiteTile(report:SiteReport,onDelete:()->Unit) {
    var open by remember(report.id){ mutableStateOf(false) }
    val score=PriorityEngine.score(report)
    Tile {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(report.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Text((if(report.access==SiteAccess.UNSAFE) "Do not enter · " else "")+score.band+" · Unverified",
                    style=MaterialTheme.typography.bodySmall)
            }
            Text(score.total.toString()+"/100",color=Green,fontWeight=FontWeight.Bold)
        }
        LinearProgressIndicator(progress={score.total/100f},modifier=Modifier.fillMaxWidth())
        Text(String.format(Locale.US,"%.5f, %.5f",report.latitude,report.longitude),
            style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={open=!open}) { Text(if(open) "Hide explanation" else "Why this priority?") }
        if(open) {
            HorizontalDivider()
            Text("Severity: "+score.severity+"/40 · Waterway estimate: "+score.waterway+"/25",
                style=MaterialTheme.typography.bodySmall)
            Text("Repeat: "+score.recurrence+"/20 · Access: "+score.access+"/15",
                style=MaterialTheme.typography.bodySmall)
            Text("Next step",color=Green,fontWeight=FontWeight.SemiBold)
            Text(PriorityEngine.nextStep(report))
            if(report.notes.isNotBlank()) Text("Notes: "+report.notes)
            report.photoUri?.let { image ->
                AndroidView(factory={ ImageView(it).apply {
                    scaleType=ImageView.ScaleType.CENTER_CROP
                } },update={ it.setImageURI(Uri.parse(image)) },
                    modifier=Modifier.fillMaxWidth().height(140.dp))
            }
            TextButton(onClick=onDelete){Icon(Icons.Default.Delete,null);Text("Remove local report")}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportForm(
    initial:Pair<Double,Double>?, photo:Uri?,
    onPhoto:()->Unit,onCancel:()->Unit,onSave:(SiteReport,(Boolean)->Unit)->Unit
) {
    var title by remember {mutableStateOf("")}
    var lat by remember(initial) {mutableStateOf(initial?.first?.toString()?:"")}
    var lon by remember(initial) {mutableStateOf(initial?.second?.toString()?:"")}
    var severity by remember {mutableIntStateOf(2)}
    var waterway by remember {mutableStateOf(WaterwayDistance.UNKNOWN)}
    var repeat by remember {mutableIntStateOf(1)}
    var access by remember {mutableStateOf(SiteAccess.PUBLIC)}
    var notes by remember {mutableStateOf("")}
    var errors by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
        Text("Record an observation",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Only report what you actually observed. The report is unverified.",
            style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(title,{title=it.take(80)},label={Text("Site name *")},
            singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(lat,{lat=it.take(24)},label={Text("Latitude *")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(lon,{lon=it.take(24)},label={Text("Longitude *")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                singleLine=true,modifier=Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text("Visible litter severity",fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Low",2 to "Medium",3 to "High").forEach { (num,label) ->
                FilterChip(selected=severity==num,onClick={severity=num},label={Text(label)})
            }
        }
        Text("Estimated distance to a waterway",fontWeight=FontWeight.SemiBold)
        var menu by remember {mutableStateOf(false)}
        ExposedDropdownMenuBox(expanded=menu,onExpandedChange={menu=it}) {
            OutlinedTextField(value=waterway.label,onValueChange={},readOnly=true,
                trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(menu)},
                modifier=Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                WaterwayDistance.entries.forEach { option ->
                    DropdownMenuItem(text={Text(option.label)},onClick={waterway=option;menu=false})
                }
            }
        }
        Text("Self-reported estimate, not a GIS measurement.",style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        Text("Times you personally observed this",fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { n -> FilterChip(selected=repeat==n,onClick={repeat=n},label={Text(n.toString())}) }
        }
        Text("Site accessibility",fontWeight=FontWeight.SemiBold)
        var accessMenu by remember {mutableStateOf(false)}
        ExposedDropdownMenuBox(expanded=accessMenu,onExpandedChange={accessMenu=it}) {
            OutlinedTextField(value=access.label,onValueChange={},readOnly=true,
                trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(accessMenu)},
                modifier=Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded=accessMenu,onDismissRequest={accessMenu=false}) {
                SiteAccess.entries.forEach { option ->
                    DropdownMenuItem(text={Text(option.label)},onClick={access=option;accessMenu=false})
                }
            }
        }
        if(access!=SiteAccess.PUBLIC) {
            Text("Do not enter restricted or unsafe sites.",color=MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(notes,{notes=it.take(500)},label={Text("Notes (optional)")},
            minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick=onPhoto) {Icon(Icons.Default.CameraAlt,null);Text("Add photo")}
        if(photo!=null) {
            Text("Local photo attached",style=MaterialTheme.typography.bodySmall)
            AndroidView(factory={ImageView(it)},update={it.setImageURI(photo)},
                modifier=Modifier.fillMaxWidth().height(110.dp))
        }
        if(errors.isNotBlank()) {
            Text(errors,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick={
            val report=SiteReport(title=title.trim(),latitude=lat.toDoubleOrNull()?:Double.NaN,
                longitude=lon.toDoubleOrNull()?:Double.NaN,severity=severity,waterwayDistance=waterway,
                repeatCount=repeat,access=access,notes=notes.trim(),photoUri=photo?.toString())
            val problems=ReportValidation.errors(report)
            if(problems.isNotEmpty()) errors=problems.joinToString("\n")
            else {
                errors=""
                busy=true
                onSave(report) { success ->
                    busy=false
                    if(!success) errors="Saving failed. Try again."
                }
            }
        },enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text(if(busy) "Saving…" else "Save site report") }
        TextButton(onClick=onCancel,modifier=Modifier.fillMaxWidth()){Text("Cancel")}
        Spacer(Modifier.height(28.dp))
    }
}
