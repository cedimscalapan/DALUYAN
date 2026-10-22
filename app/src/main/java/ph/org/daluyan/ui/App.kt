package ph.org.daluyan.ui

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.org.daluyan.data.WaterResult
import ph.org.daluyan.data.WaterwayLayer
import ph.org.daluyan.domain.*
import java.util.Locale

internal val Ink=Color(0xFF163D33)
internal val Forest=Color(0xFF1E604D)
internal val Mist=Color(0xFFF3F6F2)
internal val Soft=Color(0xFFE8EFE9)
internal val Subtle=Color(0xFF64786E)
internal val Orange=Color(0xFFAE7849)

@Composable
internal fun Panel(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Surface(color=Color.White,shape=RoundedCornerShape(22.dp),modifier=modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}
@Composable
internal fun Eyebrow(s:String) { Text(s.uppercase(Locale.US),style=MaterialTheme.typography.labelSmall,color=Subtle,fontWeight=FontWeight.Bold) }
@Composable
internal fun BigTitle(s:String) { Text(s,style=MaterialTheme.typography.headlineMedium,color=Ink,fontWeight=FontWeight.Bold) }
@Composable
internal fun Muted(s:String) { Text(s,color=Subtle,style=MaterialTheme.typography.bodySmall) }
@Composable
internal fun Tag(s:String, dark:Boolean=false) {
    Surface(color=if(dark)Ink else Soft,shape=RoundedCornerShape(12.dp)) {
        Text(s,color=if(dark)Color.White else Forest,style=MaterialTheme.typography.labelMedium,
            fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaluyanApp(
    reports:List<SiteReport>,official:List<WaterResult>,layer:WaterwayLayer?,
    selected:Pair<Double,Double>?,locationSource:LocationSource,accuracy:Float?,
    gpsInfo:String?,notice:String?,onDismissNotice:()->Unit,
    onPick:(Double,Double)->Unit,onGps:()->Unit,onNew:()->Unit,onEdit:(SiteReport)->Unit,
    onSave:(SiteReport,(Boolean)->Unit)->Unit,onCancel:()->Unit,onDelete:(SiteReport)->Unit,
    onAction:(SiteReport,ActionStatus,String,(Boolean)->Unit)->Unit,
    onExport:()->Unit,onImport:()->Unit,onSource:()->Unit,
    formOpen:Boolean,editing:SiteReport?,photo:Uri?,onCapture:()->Unit
) {
    val colors=lightColorScheme(primary=Forest,onPrimary=Color.White,secondary=Ink,background=Mist,
        surface=Color.White,onSurface=Ink,onBackground=Ink,error=Color(0xFFB34936))
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack=remember { SnackbarHostState() }
    var showAllEvidence by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(notice) {notice?.let{snack.showSnackbar(it);onDismissNotice()}}
    MaterialTheme(colorScheme=colors) {
        Scaffold(containerColor=Mist,
            topBar={
                Surface(color=Mist){
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=20.dp,end=20.dp,top=10.dp,bottom=10.dp),
                        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                        Column {
                            Text("DALUYAN",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,color=Forest)
                            Text("Environmental action planner",style=MaterialTheme.typography.labelSmall,color=Subtle)
                        }
                        Tag("CALAPAN · PILOT")
                    }
                }
            },
            bottomBar={
                NavigationBar(containerColor=Color.White,tonalElevation=0.dp) {
                    listOf(Triple("Map",Icons.Default.Map,0),Triple("Actions",Icons.Default.Assignment,1),
                        Triple("Evidence",Icons.Default.Assessment,2)).forEach{(name,icon,index)->
                        NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(icon,contentDescription=name)},
                            label={Text(name)},colors=NavigationBarItemDefaults.colors(indicatorColor=Soft,selectedIconColor=Forest,
                                selectedTextColor=Ink,unselectedIconColor=Subtle,unselectedTextColor=Subtle))
                    }
                }
            },snackbarHost={SnackbarHost(snack)}) {padding->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(start=16.dp,end=16.dp,top=12.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                when(tab) {
                    0-> {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.SpaceBetween) {
                            Column {
                                Text("Explore Calapan",style=MaterialTheme.typography.headlineSmall,
                                    fontWeight=FontWeight.Bold,color=Ink)
                                Muted("Tap anywhere on the map to mark a site")
                            }
                            Tag("${reports.size} reports")
                        }
                        Surface(color=Color.White,shape=RoundedCornerShape(22.dp)) {
                            Column {
                                Box(Modifier.fillMaxWidth()) {
                                    MapPanel(reports,selected,layer){lat,lon->onPick(lat,lon)}
                                    Surface(color=Color.White.copy(alpha=0.94f),
                                        shape=RoundedCornerShape(12.dp),
                                        modifier=Modifier.align(Alignment.TopStart).padding(12.dp)) {
                                        Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),
                                            verticalAlignment=Alignment.CenterVertically) {
                                            Icon(Icons.Default.Water,null,tint=Forest,modifier=Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if(layer==null) "Waterways unavailable" else "Mapped waterways ready",
                                                style=MaterialTheme.typography.labelMedium,color=Ink)
                                        }
                                    }
                                }
                                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment=Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(if(selected==null) "Choose a location" else "Point selected",
                                                fontWeight=FontWeight.Bold,color=Ink)
                                            Muted(selected?.let {
                                                "${"%.5f".format(Locale.US,it.first)}, ${"%.5f".format(Locale.US,it.second)}"
                                            } ?: "Tap map or use your GPS")
                                        }
                                        OutlinedIconButton(onClick=onGps,modifier=Modifier.size(48.dp)) {
                                            Icon(Icons.Default.MyLocation,contentDescription="Use my location",tint=Forest)
                                        }
                                    }
                                    gpsInfo?.let { Muted(it) }
                                    Button(onClick=onNew,modifier=Modifier.fillMaxWidth().height(54.dp),
                                        shape=RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp))
                                        Text("Record observation",fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        if(layer==null) {
                            Panel {
                                Text("Waterway layer unavailable",fontWeight=FontWeight.SemiBold)
                                Muted("You can still record an observation. Import a verified layer for GIS distance estimates.")
                                TextButton(onClick={tab=2}) { Text("Manage evidence");Icon(Icons.Default.ArrowForward,null) }
                            }
                        } else {
                            Muted("HOT/OpenStreetMap waterways are stored offline. Background map tiles need internet.")
                        }
                    }
                    1-> {
                        BigTitle("Field activity")
                        Muted("Your private reports and action notes. Not officially verified.")
                        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                            StatCard("${reports.size}","Reports",Modifier.weight(1f))
                            StatCard("${reports.count{it.status==ActionStatus.COMPLETED}}","Actions noted",Modifier.weight(1f))
                        }
                        if(reports.isEmpty())Panel{
                            Icon(Icons.Default.Place,null,tint=Forest)
                            Text("No observations yet",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                            Muted("Choose a location on the map and record the first real observation.")
                            Button(onClick={tab=0},modifier=Modifier.fillMaxWidth()) {Text("Explore map")}
                        } else {
                            OutlinedButton(onClick=onExport,modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Default.FileDownload,null);Spacer(Modifier.width(8.dp));Text("Export observations as CSV")
                            }
                            reports.sortedWith(compareByDescending<SiteReport>{it.severity}.thenByDescending{it.createdAt}).forEach{r->
                                val a=AssessmentEngine.assess(r,reports,layer?.distanceMeters(r.latitude,r.longitude))
                                ReportCard(r,a,onEdit={onEdit(r)},onDelete={onDelete(r)},onAction={s,desc,done->onAction(r,s,desc,done)})
                            }
                        }
                    }
                    else-> {
                        BigTitle("Evidence & sources")
                        Muted("Actual map data and historical government measurements")
                        Panel {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Water,null,tint=Forest,modifier=Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Calapan waterways",fontWeight=FontWeight.Bold,
                                        style=MaterialTheme.typography.titleMedium)
                                    Muted(if(layer==null) "No geographic layer" else
                                        "${layer.segments.size} mapped segments · Available offline")
                                }
                                Tag(if(layer==null) "MISSING" else "ACTIVE")
                            }
                            Muted("HOT/HDX · © OpenStreetMap contributors (ODbL). Pilot rectangle, not the city boundary. Coverage may be incomplete.")
                            TextButton(onClick=onImport) {
                                Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp))
                                Text("Import another GeoJSON")
                            }
                            Muted("Distances are straight-line proximity—not water flow, connectivity or ecological risk.")
                        }
                        Panel {
                            Eyebrow("DENR-EMB MIMAROPA")
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Water quality · 2025",fontWeight=FontWeight.Bold,
                                        style=MaterialTheme.typography.titleMedium)
                                    Muted("Historical fecal-coliform readings; NOT plastic or solid-waste measurements.")
                                }
                                Tag("${official.size} readings")
                            }
                            OutlinedButton(onClick=onSource,modifier=Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.OpenInNew,null);Spacer(Modifier.width(7.dp))
                                Text("Official publication")
                            }
                        }
                        Panel {
                            (if(showAllEvidence) official else official.take(4)).forEachIndexed { index,r ->
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.station,fontWeight=FontWeight.SemiBold,color=Ink)
                                        Muted("${r.waterbody} · ${r.period} · Guideline ${r.guideline}")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(horizontalAlignment=Alignment.End) {
                                        Text(r.value.toString(),fontWeight=FontWeight.Bold,
                                            color=if(r.value>r.guideline)Orange else Forest,
                                            style=MaterialTheme.typography.titleMedium)
                                        Text("MPN/100 mL",style=MaterialTheme.typography.labelSmall,color=Subtle)
                                    }
                                }
                                if(index<(if(showAllEvidence)official.size else minOf(4,official.size))-1)
                                    HorizontalDivider(color=Soft)
                            }
                            if(official.size>4) TextButton(onClick={showAllEvidence=!showAllEvidence},
                                modifier=Modifier.fillMaxWidth()) {
                                Text(if(showAllEvidence) "Show less" else "View all ${official.size} readings")
                                Icon(if(showAllEvidence)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                            }
                        }
                    }
                }
            }
        }
        if(formOpen) {
            ModalBottomSheet(onDismissRequest=onCancel,containerColor=Mist,dragHandle={BottomSheetDefaults.DragHandle()}) {
                ReportSheet(existing=editing,initial=selected,source=locationSource,accuracy=accuracy,
                    photo=photo,onCapture=onCapture,onCancel=onCancel,onSave=onSave)
            }
        }
    }
}

@Composable
private fun StatCard(number:String,label:String,modifier:Modifier) {
    Surface(color=Soft,shape=RoundedCornerShape(19.dp),modifier=modifier) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(number,fontWeight=FontWeight.Bold,color=Ink,style=MaterialTheme.typography.headlineMedium)
            Text(label,style=MaterialTheme.typography.labelMedium,color=Subtle)
        }
    }
}

@Composable
private fun ReportCard(
    r:SiteReport,a:Assessment,onEdit:()->Unit,onDelete:()->Unit,
    onAction:(ActionStatus,String,(Boolean)->Unit)->Unit
) {
    var expanded by remember(r.id){mutableStateOf(false)}
    var confirmDelete by remember(r.id){mutableStateOf(false)}
    var actionOpen by remember(r.id){mutableStateOf(false)}
    Panel {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=Ink)
                Muted("${r.status.title} · Unverified")
            }
            Tag(if(a.distanceMeters==null)"GIS pending" else "${a.concern}/100",dark=a.distanceMeters!=null)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Tag(if(a.distanceMeters==null)"Concern incomplete" else a.concernLabel)
            Tag(a.confidenceLabel)
        }
        Muted("${"%.5f".format(Locale.US,r.latitude)}, ${"%.5f".format(Locale.US,r.longitude)}")
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.weight(1f)) {
                Text(if(expanded)"Less detail" else "View details")
            }
            Button(onClick={actionOpen=true},modifier=Modifier.weight(1f)) {Text("Update action")}
        }
        if(expanded) {
            HorizontalDivider(color=Soft)
            Text("Why this assessment?",fontWeight=FontWeight.Bold,color=Ink)
            Muted("Visible litter level: ${r.severity}/3 · Distinct nearby report days: ${a.recurrence} · ${r.access.title}.")
            Muted(if(a.distanceMeters==null)"No trusted waterway geometry available: proximity is unknown, not low concern."
                else "Nearest imported waterway line: ${"%.0f".format(Locale.US,a.distanceMeters)} m straight-line distance (approximate).")
            Text("Suggested next step",fontWeight=FontWeight.Bold,color=Forest)
            Text(a.nextStep,style=MaterialTheme.typography.bodyMedium)
            if(r.notes.isNotBlank())Muted("Field notes: ${r.notes}")
            if(r.actionNote.isNotBlank())Muted("Self-reported action: ${r.actionNote}")
            Row {
                TextButton(onClick=onEdit){Icon(Icons.Default.Edit,null);Spacer(Modifier.width(5.dp));Text("Edit")}
                TextButton(onClick={confirmDelete=true}){Icon(Icons.Default.Delete,null);Spacer(Modifier.width(5.dp));Text("Delete")}
            }
        }
    }
    if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete observation?")},
        text={Text("This removes the private local record and its locally captured photo. Exported CSV files will not change.")},
        confirmButton={TextButton(onClick={confirmDelete=false;onDelete()}){Text("Delete",color=MaterialTheme.colorScheme.error)}},
        dismissButton={TextButton(onClick={confirmDelete=false}){Text("Cancel")}})
    if(actionOpen) ActionDialog(r,onDismiss={actionOpen=false},onSave={s,n,done->onAction(s,n){ok->if(ok)actionOpen=false;done(ok)}})
}

@Composable
private fun ActionDialog(r:SiteReport,onDismiss:()->Unit,onSave:(ActionStatus,String,(Boolean)->Unit)->Unit) {
    var status by remember(r.id){mutableStateOf(r.status)}
    var note by remember(r.id){mutableStateOf(r.actionNote)}
    var busy by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},
        title={Text("Record an action")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Muted("Self-reported only. This does not confer official review or authorization.")
            ActionStatus.entries.forEach{s->
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().clickable{status=s}) {
                    RadioButton(selected=status==s,onClick={status=s})
                    Text(s.title)
                }
            }
            OutlinedTextField(note,{note=it.take(800)},label={Text(if(status==ActionStatus.COMPLETED)"Result details *" else "Action notes")},
                minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth(),supportingText={Text("Required for recorded completion; describe what happened.")})
        }},
        confirmButton={Button(enabled=!busy && (status!=ActionStatus.COMPLETED || note.trim().length>=12),onClick={
            busy=true;onSave(status,note){busy=false}
        }){Text("Save status")}},
        dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Cancel")}})
}
