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
    LaunchedEffect(notice) {notice?.let{snack.showSnackbar(it);onDismissNotice()}}
    MaterialTheme(colorScheme=colors) {
        Scaffold(containerColor=Mist,
            topBar={
                Surface(color=Mist){
                    Row(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=18.dp,bottom=12.dp),
                        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                        Column {
                            Text("DALUYAN",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black,color=Forest)
                            Text("FROM EVIDENCE TO ACTION",style=MaterialTheme.typography.labelSmall,color=Subtle)
                        }
                        Tag("PILOT · CALAPAN")
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
                .padding(start=18.dp,end=18.dp,top=12.dp,bottom=22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                when(tab) {
                    0-> {
                        Eyebrow("Your surroundings")
                        BigTitle("Map the concern.")
                        Muted("Explore real places. Tap any point to record an observation.")
                        Panel {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Calapan area",fontWeight=FontWeight.Bold,color=Ink)
                                    Muted("Live map · internet needed for tiles")
                                }
                                Tag("${reports.size} sites")
                            }
                            Surface(shape=RoundedCornerShape(16.dp),color=Soft){
                                MapPanel(reports,selected){lat,lon->onPick(lat,lon)}
                            }
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick=onGps,modifier=Modifier.weight(1f)) {
                                    Icon(Icons.Default.MyLocation,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("My location")
                                }
                                if(selected!=null) Tag("Point selected") else Tag("Tap map")
                            }
                            selected?.let{Muted("Selected: ${"%.5f".format(Locale.US,it.first)}, ${"%.5f".format(Locale.US,it.second)}")}
                            gpsInfo?.let{Muted(it)}
                        }
                        Panel {
                            Eyebrow("Quick action")
                            Text("See litter? Document it.",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,color=Ink)
                            Muted("A short, private field report. No account or internet needed after a point is chosen.")
                            Button(onClick=onNew,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(16.dp),
                                shape=RoundedCornerShape(14.dp)) {
                                Icon(Icons.Default.Add,null);Spacer(Modifier.width(7.dp));Text("Record observation",fontWeight=FontWeight.Bold)
                            }
                            Muted("Do not enter private property or unsafe locations.")
                        }
                        if(layer==null) Panel {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Layers,null,tint=Orange)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Waterway data not loaded",fontWeight=FontWeight.Bold)
                                    Muted("Import verified GeoJSON for local GIS distances.")
                                }
                            }
                            TextButton(onClick={tab=2}) {Text("Go to Evidence");Icon(Icons.Default.ArrowForward,null)}
                        }
                    }
                    1-> {
                        Eyebrow("Community observations")
                        BigTitle("From report to action.")
                        Muted("Only personal field notes. No site or action is officially verified.")
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
                        Eyebrow("Transparent sources")
                        BigTitle("Evidence, not assumptions.")
                        Muted("Understand where every measurement comes from and what it can tell us.")
                        Panel {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Layers,null,tint=Forest)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Waterway geometry",fontWeight=FontWeight.Bold)
                                    Muted(if(layer==null)"No imported river lines" else "${layer.segments.size} mapped line segments")
                                }
                            }
                            Muted("Import a waterway-only GeoJSON FeatureCollection from a verified GIS source (maximum 3 MB). GIS distance is straight-line proximity, not water flow or drainage connectivity.")
                            Button(onClick=onImport,modifier=Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp));Text(if(layer==null)"Import waterway GeoJSON" else "Replace waterway layer")
                            }
                            Muted("Suggested source: OpenStreetMap Overpass Turbo, exporting waterway=river/stream lines. Check local coverage and licensing before use.")
                        }
                        Panel {
                            Eyebrow("Government monitoring")
                            Text("Calapan water quality · 2025",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            Muted("DENR-EMB MIMAROPA historical fecal-coliform readings (MPN/100 mL). They do NOT measure solid-waste volume or live pollution.")
                            OutlinedButton(onClick=onSource,modifier=Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.OpenInNew,null);Spacer(Modifier.width(7.dp));Text("View official publication")
                            }
                        }
                        official.forEach{r->
                            Panel {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.station,fontWeight=FontWeight.Bold,color=Ink)
                                        Muted("${r.waterbody} · Class ${r.classification} · ${r.period}")
                                    }
                                    Column(horizontalAlignment=Alignment.End) {
                                        Text(r.value.toString(),fontWeight=FontWeight.Bold,
                                            style=MaterialTheme.typography.titleLarge,color=if(r.value>r.guideline)Orange else Forest)
                                        Text("MPN/100 mL",style=MaterialTheme.typography.labelSmall,color=Subtle)
                                    }
                                }
                                Muted("Published guideline: ${r.guideline} ${r.unit}. No verified station coordinates supplied.")
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
