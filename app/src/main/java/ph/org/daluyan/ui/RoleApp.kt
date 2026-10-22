package ph.org.daluyan.ui

import android.net.Uri
import android.widget.ImageView
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ph.org.daluyan.data.WaterResult
import ph.org.daluyan.data.WaterwayLayer
import ph.org.daluyan.domain.*
import ph.org.daluyan.workflow.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF193D35)
private val Pine = Color(0xFF187355)
private val Cream = Color(0xFFF6F8F5)
private val Haze = Color(0xFFEDF3EF)
private val Fog = Color(0xFF65776E)
private val BorderColor = Color(0xFFE1E9E3)
private val Amber = Color(0xFFAA6F32)

private fun prettyDate(utc: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(utc))
private fun navFor(role: DemoRole) = when(role) {
    DemoRole.GUEST -> listOf("Explore","Learn","About")
    DemoRole.COMMUNITY -> listOf("Explore","My reports","Updates")
    DemoRole.BARANGAY -> listOf("Review","Map","Cases")
    DemoRole.COORDINATOR -> listOf("Overview","Cases","Planning")
    DemoRole.FIELD -> listOf("My tasks","Map","History")
    DemoRole.ADMIN -> listOf("Accounts","GIS data","Audit")
}
private fun navIcon(index: Int) = when(index) {0->Icons.Default.Explore;1->Icons.Default.ListAlt;else->Icons.Default.Assessment}

@Composable private fun SurfaceCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Surface(modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=Color.White,
        tonalElevation=0.dp,border=BorderStroke(1.dp,BorderColor)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}
@Composable private fun SmallLabel(value:String) {Text(value.uppercase(Locale.US),color=Fog,
    style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold)}
@Composable private fun Sub(value:String){Text(value,color=Fog,style=MaterialTheme.typography.bodySmall)}
@Composable private fun Header(title:String,subtitle:String) {
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(title,color=Navy,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Sub(subtitle)
    }
}
@Composable private fun Chip(value:String,attention:Boolean=false) {
    Surface(color=if(attention)Color(0xFFFFF1E2) else Haze,shape=RoundedCornerShape(10.dp)){
        Text(value,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp),
            color=if(attention)Amber else Pine,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Medium)
    }
}
@Composable private fun EmptyPanel(title:String,description:String,onClick:(()->Unit)?=null,button:String="") {
    SurfaceCard {
        Icon(Icons.Default.Spa,null,tint=Pine,modifier=Modifier.size(30.dp))
        Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,color=Navy)
        Sub(description)
        if(onClick!=null) Button(onClick=onClick,modifier=Modifier.fillMaxWidth()) {Text(button)}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleApp(
    role: DemoRole,onRoleChange:(DemoRole)->Unit,
    reports:List<SiteReport>, cases:List<CaseItem>, events:List<CaseEvent>,
    official:List<WaterResult>,layer:WaterwayLayer?, selected:Pair<Double,Double>?,
    locationSource:LocationSource,accuracy:Float?,gpsInfo:String?,notice:String?,onDismissNotice:()->Unit,
    onPick:(Double,Double)->Unit,onGps:()->Unit,onNew:()->Unit,onEdit:(SiteReport)->Unit,
    onSave:(SiteReport,(Boolean)->Unit)->Unit,onCancel:()->Unit,onDelete:(SiteReport)->Unit,
    onSelectCase:(Long)->Unit,onTransition:(Long,CaseStage,String,String,(String?)->Unit)->Unit,
    onExport:()->Unit,onImport:()->Unit,onSource:()->Unit,
    onExportGeoJson:()->Unit,onExportGeoPackage:()->Unit,
    formOpen:Boolean,editing:SiteReport?,photo:Uri?,onCapture:()->Unit
) {
    val scheme=lightColorScheme(primary=Pine,onPrimary=Color.White,background=Cream,onBackground=Navy,
        surface=Color.White,onSurface=Navy,secondary=Navy,error=Color(0xFFAC493D))
    var tab by rememberSaveable {mutableIntStateOf(0)}
    var accountMenu by remember {mutableStateOf(false)}
    var selectedCase by remember {mutableStateOf<Long?>(null)}
    var showAllReadings by rememberSaveable {mutableStateOf(false)}
    val snackbar=remember {SnackbarHostState()}
    LaunchedEffect(role) {tab=0;selectedCase=null}
    LaunchedEffect(notice) {notice?.let {snackbar.showSnackbar(it);onDismissNotice()}}
    val joined=remember(reports,cases) {cases.mapNotNull{c->reports.find{it.id==c.reportId}?.let{it to c}}}
    fun launchCase(id:Long) {selectedCase=id;onSelectCase(id)}
    val activeCase=selectedCase?.let {id->joined.firstOrNull {it.second.reportId==id}}
    MaterialTheme(colorScheme=scheme) {
        Scaffold(containerColor=Cream,
            topBar={
                Surface(color=Color.White) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=18.dp,vertical=10.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Surface(color=Pine,shape=RoundedCornerShape(12.dp),modifier=Modifier.size(37.dp)) {
                            Box(contentAlignment=Alignment.Center) {Icon(Icons.Default.Waves,null,tint=Color.White)}
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("DALUYAN",color=Navy,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleMedium)
                            Text("CALAPAN · ENVIRONMENT",color=Fog,style=MaterialTheme.typography.labelSmall)
                        }
                        Surface(color=Haze,shape=RoundedCornerShape(50.dp),
                            modifier=Modifier.clickable {accountMenu=true}) {
                            Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle,null,tint=Pine,modifier=Modifier.size(19.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Demo",style=MaterialTheme.typography.labelMedium,color=Navy)
                                Icon(Icons.Default.ExpandMore,null,tint=Fog,modifier=Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            bottomBar={NavigationBar(containerColor=Color.White,tonalElevation=0.dp) {
                navFor(role).forEachIndexed {index,label->
                    NavigationBarItem(selected=tab==index,onClick={tab=index},
                        icon={Icon(navIcon(index),contentDescription=label)},label={Text(label)},
                        colors=NavigationBarItemDefaults.colors(selectedIconColor=Pine,selectedTextColor=Pine,
                            indicatorColor=Haze,unselectedIconColor=Fog,unselectedTextColor=Fog))
                }
            }},
            snackbarHost={SnackbarHost(snackbar)}
        ) {paddings->
            Column(Modifier.fillMaxSize().padding(paddings).verticalScroll(rememberScrollState())
                .padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                RoleBanner(role)
                when(role) {
                    DemoRole.GUEST -> when(tab) {
                        0 -> MapWorkspace(emptyList(),layer,selected,gpsInfo,onPick,onGps,false,{})
                        1 -> EvidenceWorkspace(official,layer,onImport,onSource,onExportGeoJson,onExportGeoPackage,showAllReadings,{showAllReadings=!showAllReadings})
                        else -> EmptyPanel("Explore without an account","This is a local demonstration. Community submissions and organizational accounts require a secure shared service. Switch to the Community demo profile to record a private observation.",onClick={accountMenu=true},button="Choose demo role")
                    }
                    DemoRole.COMMUNITY -> when(tab) {
                        0 -> MapWorkspace(reports,layer,selected,gpsInfo,onPick,onGps,true,onNew)
                        1 -> {
                            Header("My reports","Personal observations and their demonstration case status")
                            if(joined.isEmpty()) EmptyPanel("Nothing reported yet","Select a point on the map to document a concern.",onClick={tab=0},button="Explore map")
                            else joined.forEach {(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}
                            OutlinedButton(onClick=onExport,modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.FileDownload,null);Spacer(Modifier.width(6.dp));Text("Export local observations")}
                        }
                        else -> {
                            Header("Case updates","Follow what happens after your report")
                            if(joined.isEmpty()) EmptyPanel("No updates","A case timeline appears after you create your first report.")
                            else joined.forEach{(r,c)->SurfaceCard {
                                Text(r.title,fontWeight=FontWeight.Bold)
                                Chip(c.stage.publicLabel)
                                Sub("Updated ${prettyDate(c.updatedAt)}")
                                TextButton(onClick={launchCase(c.reportId)}) {Text("View timeline");Icon(Icons.Default.ArrowForward,null)}
                            }}
                        }
                    }
                    DemoRole.BARANGAY -> when(tab) {
                        0 -> {
                            Header("Review queue","Validate observations before forwarding them")
                            SummaryCounters(listOf("Awaiting" to joined.count{it.second.stage==CaseStage.SUBMITTED},"Clarification" to joined.count{it.second.stage==CaseStage.CLARIFICATION}))
                            val queue=joined.filter{it.second.stage in listOf(CaseStage.SUBMITTED,CaseStage.CLARIFICATION)}
                            if(queue.isEmpty()) EmptyPanel("Review queue is clear","New local demonstration reports will appear here.")
                            else queue.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}
                        }
                        1 -> MapWorkspace(reports,layer,selected,gpsInfo,onPick,onGps,false,{})
                        else -> {
                            Header("Barangay cases","All local pilot observations and review decisions")
                            if(joined.isEmpty())EmptyPanel("No cases","Records created in the community profile will show here.")
                            else joined.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}
                        }
                    }
                    DemoRole.COORDINATOR -> when(tab) {
                        0 -> {
                            Header("City overview","Review case priorities and accountable interventions")
                            SummaryCounters(listOf("Cases" to joined.size,"Verified" to joined.count{it.second.stage==CaseStage.VERIFIED},"Active" to joined.count{it.second.stage in listOf(CaseStage.PLANNED,CaseStage.ASSIGNED,CaseStage.IN_PROGRESS,CaseStage.AWAITING_REVIEW)},"Closed" to joined.count{it.second.stage==CaseStage.CLOSED}))
                            SmallLabel("Needs attention")
                            val priority=joined.filter {it.second.stage in listOf(CaseStage.VERIFIED,CaseStage.FOLLOW_UP,CaseStage.AWAITING_REVIEW)}
                                .sortedByDescending{AssessmentEngine.assess(it.first,reports,layer?.distanceMeters(it.first.latitude,it.first.longitude)).concern}
                            if(priority.isEmpty())EmptyPanel("No cases awaiting planning","Review and accept a community observation using the Barangay demo profile first.")
                            else priority.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}
                        }
                        1 -> {Header("All cases","Track the complete environmental response")
                            if(joined.isEmpty()) EmptyPanel("No cases","Community observations appear here after recording.")
                            else joined.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}}
                        else -> {Header("Intervention planning","Only reviewed reports can become assigned work")
                            val actionable=joined.filter{it.second.stage in listOf(CaseStage.VERIFIED,CaseStage.PLANNED,CaseStage.ASSIGNED,CaseStage.IN_PROGRESS,CaseStage.AWAITING_REVIEW,CaseStage.FOLLOW_UP)}
                            if(actionable.isEmpty())EmptyPanel("No active intervention plans","Review a new observation and plan a safe response.")
                            else actionable.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}}
                    }
                    DemoRole.FIELD -> when(tab) {
                        0 -> {Header("My assignments","Only cases assigned to the pilot field team")
                            val assigned=joined.filter{it.second.assignee.isNotBlank() && it.second.stage in listOf(CaseStage.ASSIGNED,CaseStage.IN_PROGRESS)}
                            if(assigned.isEmpty())EmptyPanel("No assigned work","The coordinator must review, plan and assign a case before it appears here.")
                            else assigned.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}}
                        1 -> MapWorkspace(reports.filter{r->cases.any{it.reportId==r.id && it.assignee.isNotBlank()}},layer,selected,gpsInfo,onPick,onGps,false,{})
                        else -> {Header("Task history","Submitted work and previously completed activities")
                            val history=joined.filter{it.second.assignee.isNotBlank() && it.second.stage in listOf(CaseStage.AWAITING_REVIEW,CaseStage.CLOSED,CaseStage.FOLLOW_UP)}
                            if(history.isEmpty())EmptyPanel("No task history","Your submissions will appear after you update assigned work.")
                            else history.forEach{(r,c)->CaseCard(r,c,layer,role){launchCase(c.reportId)}}}
                    }
                    DemoRole.ADMIN -> when(tab) {
                        0 -> {Header("Demo profiles","Account provisioning is not connected to a server")
                            DemoRole.entries.forEach {profile-> SurfaceCard {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Icon(Icons.Default.Badge,null,tint=Pine)
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {Text(profile.label,fontWeight=FontWeight.Bold);Sub(profile.description)}
                                    Chip("LOCAL")
                                }
                            }}
                            Sub("These selectable roles are demonstration profiles, not authenticated or LGU-authorized accounts. A backend must enforce real access rules.")
                        }
                        1 -> EvidenceWorkspace(official,layer,onImport,onSource,onExportGeoJson,onExportGeoPackage,showAllReadings,{showAllReadings=!showAllReadings})
                        else -> {Header("Case activity log","Locally recorded demonstration status changes")
                            if(joined.isEmpty())EmptyPanel("No audit events","Create a report to start a case timeline.")
                            else joined.forEach {(_,c)->SurfaceCard {
                                Text("Case #${c.reportId}",fontWeight=FontWeight.Bold)
                                Chip(c.stage.title)
                                Sub("${c.lastNote} · ${prettyDate(c.updatedAt)}")
                                TextButton(onClick={launchCase(c.reportId)}) {Text("Inspect full history")}
                            }}
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        if(accountMenu) AlertDialog(onDismissRequest={accountMenu=false},title={Text("Choose a demo workspace")},
            text={Column(Modifier.verticalScroll(rememberScrollState())) {
                Sub("Switch roles to test the same local case from different perspectives. No official account is being authenticated.")
                DemoRole.entries.forEach {item->
                    Row(Modifier.fillMaxWidth().clickable {onRoleChange(item);accountMenu=false}.padding(vertical=9.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=role==item,onClick={onRoleChange(item);accountMenu=false})
                        Spacer(Modifier.width(6.dp))
                        Column{Text(item.label,fontWeight=FontWeight.Medium);Sub(item.description)}
                    }
                }
            }},confirmButton={TextButton(onClick={accountMenu=false}){Text("Close")}})
        if(formOpen && role==DemoRole.COMMUNITY) ModalBottomSheet(onDismissRequest=onCancel,containerColor=Cream) {
            ReportSheet(existing=editing,initial=selected,source=locationSource,accuracy=accuracy,
                photo=photo,onCapture=onCapture,onCancel=onCancel,onSave=onSave)
        }
        activeCase?.let{(report,case)->
            ModalBottomSheet(onDismissRequest={selectedCase=null},containerColor=Cream) {
                CaseDetail(report,case,events,role,reports,layer,
                    onClose={selectedCase=null},onEdit={onEdit(report);selectedCase=null},
                    onDelete={onDelete(report);selectedCase=null},
                    onTransition={to,note,assignee,cb->onTransition(report.id,to,note,assignee,cb)})
            }
        }
    }
}

@Composable private fun RoleBanner(role:DemoRole) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SmallLabel(if(role==DemoRole.GUEST)"PUBLIC EXPLORE" else "${role.label} · local demo")
            Text(when(role){
                DemoRole.GUEST->"Explore your community"
                DemoRole.COMMUNITY->"Every observation matters"
                DemoRole.BARANGAY->"Review with confidence"
                DemoRole.COORDINATOR->"From evidence to action"
                DemoRole.FIELD->"Make progress visible"
                DemoRole.ADMIN->"Manage with clarity"
            },fontWeight=FontWeight.Bold,color=Navy,style=MaterialTheme.typography.titleLarge)
        }
        Surface(color=Haze,shape=RoundedCornerShape(10.dp)) {
            Text("PILOT",modifier=Modifier.padding(horizontal=9.dp,vertical=7.dp),
                color=Pine,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
        }
    }
}
@Composable private fun SummaryCounters(values:List<Pair<String,Int>>) {
    values.chunked(2).forEach {chunk->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        chunk.forEach {(name,n)->Surface(color=Haze,shape=RoundedCornerShape(15.dp),modifier=Modifier.weight(1f)) {
            Column(Modifier.padding(15.dp)) {
                Text(n.toString(),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=Navy)
                Sub(name)
            }
        }}
        if(chunk.size==1)Spacer(Modifier.weight(1f))
    }}
}
@Composable private fun MapWorkspace(reports:List<SiteReport>,layer:WaterwayLayer?,selected:Pair<Double,Double>?,gpsInfo:String?,
    onPick:(Double,Double)->Unit,onGps:()->Unit,canReport:Boolean,onNew:()->Unit) {
    Header("Community map","Real waterways and location-based observations")
    Surface(color=Color.White,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,BorderColor)) {
        Column {
            MapPanel(reports,selected,layer,onPick)
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if(selected==null)"Select a point on the map" else "Location selected",fontWeight=FontWeight.Bold)
                        Sub(selected?.let{String.format(Locale.US,"%.5f, %.5f",it.first,it.second)}?:"Tap the map or use GPS")
                    }
                    OutlinedIconButton(onClick=onGps,modifier=Modifier.size(48.dp)) {
                        Icon(Icons.Default.MyLocation,"Use GPS",tint=Pine)
                    }
                }
                gpsInfo?.let{Sub(it)}
                if(canReport)Button(onClick=onNew,modifier=Modifier.fillMaxWidth().height(52.dp),
                    shape=RoundedCornerShape(13.dp)) {
                    Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Record observation")
                }
            }
        }
    }
    SurfaceCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Water,null,tint=Pine)
            Spacer(Modifier.width(8.dp))
            Column {Text("Geospatial evidence",fontWeight=FontWeight.Bold)
                Sub(if(layer==null)"No imported waterways" else "${layer.segments.size} mapped segments · available offline")}
        }
        Sub("Nearby mapped water does not prove pollution transport. Background map tiles need internet.")
    }
}
@Composable private fun EvidenceWorkspace(official:List<WaterResult>,layer:WaterwayLayer?,onImport:()->Unit,onSource:()->Unit,
    onExportGeoJson:()->Unit,onExportGeoPackage:()->Unit, expanded:Boolean,onToggle:()->Unit) {
    Header("Data & sources","Transparent environmental evidence, not assumptions")
    SurfaceCard {
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Water,null,tint=Pine);Spacer(Modifier.width(8.dp))
            Text("Calapan waterway network",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Chip(if(layer==null)"Missing" else "Ready")}
        Sub(if(layer==null)"No usable waterways on this device" else "${layer.segments.size} line segments bundled for offline GIS distance calculations")
        Sub("HOT/HDX · © OpenStreetMap contributors (ODbL). A pilot rectangle, not the official city boundary.")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onExportGeoJson,modifier=Modifier.weight(1f)){Text("GeoJSON")}
            OutlinedButton(onClick=onExportGeoPackage,modifier=Modifier.weight(1f)){Text("GeoPackage")}
        }
        TextButton(onClick=onImport){Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(7.dp));Text("Import a different GeoJSON")}
        Sub("Proximity is a straight-line GIS measurement, not a drainage or ecological risk forecast.")
    }
    SurfaceCard {
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.FactCheck,null,tint=Pine);Spacer(Modifier.width(7.dp))
            Text("DENR monitoring · 2025",fontWeight=FontWeight.Bold)}
        Sub("Historic fecal-coliform readings. They are NOT measurements of plastic litter or solid-waste volume.")
        OutlinedButton(onClick=onSource,modifier=Modifier.fillMaxWidth()){Text("Open official source")}
        (if(expanded)official else official.take(4)).forEach{reading->
            HorizontalDivider(color=BorderColor)
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){
                Text(reading.station,fontWeight=FontWeight.Medium)
                Sub("${reading.waterbody} · ${reading.period} · guideline ${reading.guideline}")
            }
                Text(reading.value.toString(),fontWeight=FontWeight.Bold,color=if(reading.value>reading.guideline)Amber else Pine)
            }
        }
        if(official.size>4)TextButton(onClick=onToggle,modifier=Modifier.fillMaxWidth()){
            Text(if(expanded)"Show fewer readings" else "View all ${official.size} readings")}
    }
}
@Composable private fun CaseCard(r:SiteReport,c:CaseItem,layer:WaterwayLayer?,role:DemoRole,onClick:()->Unit) {
    SurfaceCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.title,fontWeight=FontWeight.Bold,color=Navy,style=MaterialTheme.typography.titleMedium,
                    maxLines=2,overflow=TextOverflow.Ellipsis)
                Sub("Case #${r.id} · ${prettyDate(r.createdAt)}")
            }
            Chip(c.stage.publicLabel,c.stage==CaseStage.CLARIFICATION||c.stage==CaseStage.FOLLOW_UP)
        }
        val geo=remember(r,layer) {layer?.distanceMeters(r.latitude,r.longitude)}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Chip("Litter: ${listOf("","Low","Moderate","High")[r.severity.coerceIn(1,3)]}")
            Chip(geo?.let{String.format(Locale.US,"%.0f m to mapped water",it)}?:"GIS unavailable")
        }
        if(c.assignee.isNotBlank())Sub("Assigned: ${c.assignee}")
        Button(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){
            Text(if(CaseRules.next(role,c.stage).isNotEmpty())"Review case and act" else "View case details")
            Spacer(Modifier.width(6.dp));Icon(Icons.Default.ArrowForward,null,modifier=Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CaseDetail(report:SiteReport,c:CaseItem,events:List<CaseEvent>,role:DemoRole,
    allReports:List<SiteReport>,layer:WaterwayLayer?,onClose:()->Unit,onEdit:()->Unit,onDelete:()->Unit,
    onTransition:(CaseStage,String,String,(String?)->Unit)->Unit) {
    var next by remember(c.reportId,c.stage){mutableStateOf<CaseStage?>(null)}
    var reason by remember(c.reportId,c.stage){mutableStateOf("")}
    var assignee by remember(c.reportId,c.stage){mutableStateOf("Calapan pilot field team")}
    var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).navigationBarsPadding().padding(horizontal=18.dp,vertical=8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {SmallLabel("CASE #${report.id} · DEMONSTRATION");
                Text(report.title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,color=Navy)}
            IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close")}
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            SurfaceCard {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Current status",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Chip(c.stage.title)
                }
                Sub("Coordinates: ${String.format(Locale.US,"%.5f, %.5f",report.latitude,report.longitude)}")
                Sub("Evidence is community-submitted, not independently verified by an actual LGU.")
                val a=AssessmentEngine.assess(report,allReports,layer?.distanceMeters(report.latitude,report.longitude))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Chip("Concern ${a.concern}/100");Chip("Evidence ${a.confidence}/100")
                }
                Sub(a.nextStep)
                if(report.notes.isNotBlank())Text(report.notes)
                report.photoUri?.let {uri->
                    AndroidView(factory={ImageView(it).apply{scaleType=ImageView.ScaleType.CENTER_CROP}},
                        update={it.setImageURI(Uri.parse(uri))},modifier=Modifier.fillMaxWidth().height(170.dp))
                }
            }
            SurfaceCard {
                Text("Case history",fontWeight=FontWeight.Bold)
                if(events.isEmpty())Sub("No events yet.")
                events.forEach{e->Row(verticalAlignment=Alignment.Top){
                    Icon(Icons.Default.CheckCircle,null,tint=Pine,modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Column{Text(e.to.title,fontWeight=FontWeight.Medium);Sub("${e.actor.label} · ${prettyDate(e.timestamp)}");Sub(e.note)}
                }}
            }
            if(role==DemoRole.COMMUNITY && c.stage in listOf(CaseStage.SUBMITTED,CaseStage.CLARIFICATION)) {
                OutlinedButton(onClick=onEdit,modifier=Modifier.fillMaxWidth()){Text("Edit my observation")}
                if(c.stage==CaseStage.SUBMITTED)TextButton(onClick=onDelete){Text("Delete local case",color=MaterialTheme.colorScheme.error)}
            }
            val permitted=CaseRules.next(role,c.stage)
            if(permitted.isNotEmpty())SurfaceCard {
                Text("Available next steps",fontWeight=FontWeight.Bold)
                permitted.forEach{stage->OutlinedButton(onClick={next=stage;reason="";error=null},modifier=Modifier.fillMaxWidth()){
                    Text(stage.title)
                }}
            }
        }
    }
    if(next!=null) AlertDialog(onDismissRequest={if(!busy)next=null},title={Text(next!!.title)},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Sub("Local role simulation. This is not an authenticated or legally binding government decision.")
            if(next==CaseStage.ASSIGNED) OutlinedTextField(assignee,{assignee=it.take(80)},label={Text("Field team *")},singleLine=true)
            OutlinedTextField(reason,{reason=it.take(800)},label={Text("Decision or outcome explanation *")},
                minLines=3,maxLines=6,modifier=Modifier.fillMaxWidth())
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }},confirmButton={Button(onClick={
            val destination=next!!
            val issue=CaseRules.error(role,c,destination,reason,assignee)
            if(issue!=null) error=issue else {
                busy=true
                onTransition(destination,reason,assignee){message->busy=false;if(message==null){next=null;reason=""}else error=message}
            }
        },enabled=!busy){Text(if(busy)"Saving…" else "Confirm")}},
        dismissButton={TextButton(onClick={next=null},enabled=!busy){Text("Cancel")}})
}
