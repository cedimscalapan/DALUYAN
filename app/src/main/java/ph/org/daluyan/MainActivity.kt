package ph.org.daluyan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import ph.org.daluyan.data.*
import ph.org.daluyan.domain.*
import ph.org.daluyan.ui.DaluyanApp
import java.io.File
import java.io.ByteArrayOutputStream

class MainActivity:ComponentActivity() {
    private val database by lazy{ReportDatabase(applicationContext)}

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext)
        setContent {
            val scope=rememberCoroutineScope()
            val reports=remember{mutableStateListOf<SiteReport>()}
            val evidence=remember{runCatching{OfficialData.read(applicationContext)}.getOrDefault(emptyList())}
            var layer by remember{mutableStateOf(WaterwayLayer.load(applicationContext))}
            var selected by remember{mutableStateOf<Pair<Double,Double>?>(null)}
            var selectedSource by remember{mutableStateOf(LocationSource.MAP)}
            var selectedAccuracy by remember{mutableStateOf<Float?>(null)}
            var editor by remember{mutableStateOf<SiteReport?>(null)}
            var formOpen by remember{mutableStateOf(false)}
            var photo by remember{mutableStateOf<Uri?>(null)}
            var proposed by remember{mutableStateOf<Uri?>(null)}
            var notice by remember{mutableStateOf<String?>(null)}
            var gpsInfo by remember{mutableStateOf<String?>(null)}
            var pendingCsv by remember{mutableStateOf("")}

            suspend fun reload() {
                val data=withContext(Dispatchers.IO){database.all()}
                reports.clear();reports.addAll(data)
            }
            LaunchedEffect(Unit){try{reload()}catch(e:Exception){notice="Stored reports could not be loaded."}}
            val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
                if(uri!=null)scope.launch {
                    try {
                        withContext(Dispatchers.IO){
                            contentResolver.openOutputStream(uri)?.use{it.write(pendingCsv.toByteArray(Charsets.UTF_8))}
                                ?:error("No export destination")
                        }
                        notice="CSV exported to your selected folder."
                    } catch(e:Exception){notice="Could not export CSV."}
                }
            }
            val geoLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
                if(uri!=null)scope.launch {
                    try {
                        val text=withContext(Dispatchers.IO){
                            contentResolver.openInputStream(uri)?.use{stream->
                                val out=ByteArrayOutputStream()
                                val buf=ByteArray(8192)
                                while(true){
                                    val n=stream.read(buf)
                                    if(n<0)break
                                    out.write(buf,0,n)
                                    require(out.size()<=3*1024*1024){"File exceeds 3 MB."}
                                }
                                out.toString("UTF-8")
                            }?:error("Unable to read file")
                        }
                        layer=withContext(Dispatchers.IO){WaterwayLayer.save(applicationContext,text)}
                        notice="Waterway layer loaded. Distances are approximate straight-line measurements."
                    } catch(e:Exception){notice="Import failed: ${e.message ?: "Invalid GeoJSON"}"}
                }
            }
            val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->
                if(ok)photo=proposed
                else {proposed?.let{runCatching{contentResolver.delete(it,null,null)}};proposed=null}
            }
            fun discardNewPhoto() {
                if(photo?.toString()!=editor?.photoUri){photo?.let{runCatching{contentResolver.delete(it,null,null)}}}
                if(proposed!=photo)proposed?.let{runCatching{contentResolver.delete(it,null,null)}}
                photo=null;proposed=null
            }
            val capture={
                try {
                    val dir=File(filesDir,"evidence").apply{mkdirs()}
                    val temp=File.createTempFile("observation_",".jpg",dir)
                    proposed=FileProvider.getUriForFile(this,"$packageName.fileprovider",temp)
                    camera.launch(proposed!!)
                } catch(e:Exception){notice="Camera is not available."}
            }
            fun receiveLocation(loc:Location?) {
                if(loc==null){gpsInfo="GPS location unavailable. Select a place on the map.";return}
                if(!loc.latitude.isFinite() || !loc.longitude.isFinite()){gpsInfo="Location has invalid coordinates.";return}
                selected=loc.latitude to loc.longitude
                selectedSource=LocationSource.GPS
                selectedAccuracy=if(loc.hasAccuracy())loc.accuracy else null
                gpsInfo=selectedAccuracy?.let{"GPS accuracy approximately ${it.toInt()} m. Confirm the point on the map."}
                    ?: "GPS point selected. Confirm the location."
            }
            fun fetchLocation() {
                try {
                    val service=getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val provider=listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER)
                        .firstOrNull{service.isProviderEnabled(it)}
                    if(provider==null){gpsInfo="Location services are disabled.";return}
                    if(Build.VERSION.SDK_INT>=30){
                        service.getCurrentLocation(provider,null,ContextCompat.getMainExecutor(this)){receiveLocation(it)}
                    } else {
                        @Suppress("DEPRECATION")
                        val recent=service.getLastKnownLocation(provider)
                        if(recent!=null && System.currentTimeMillis()-recent.time < 60_000)receiveLocation(recent)
                        else gpsInfo="No recent GPS fix. Tap a point on the map."
                    }
                }catch(e:SecurityException){gpsInfo="Location permission denied."}
                catch(e:Exception){gpsInfo="GPS unavailable. Select the place manually."}
            }
            val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed->
                if(allowed)fetchLocation() else gpsInfo="Permission denied. Select the location on the map."
            }
            DaluyanApp(
                reports=reports.toList(),official=evidence,layer=layer,
                selected=selected,locationSource=selectedSource,accuracy=selectedAccuracy,
                gpsInfo=gpsInfo,notice=notice,onDismissNotice={notice=null},
                onPick={lat,lon->selected=lat to lon;selectedSource=LocationSource.MAP;selectedAccuracy=null;gpsInfo=null},
                onGps={
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                        fetchLocation()
                    else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onNew={editor=null;discardNewPhoto();formOpen=true},
                onEdit={item->discardNewPhoto();editor=item;photo=item.photoUri?.let(Uri::parse);formOpen=true;selected=item.latitude to item.longitude},
                onSave={item,finished->
                    scope.launch {
                        try {
                            val ok=withContext(Dispatchers.IO){if(item.id==0L){database.insert(item);true}else database.update(item)}
                            if(!ok)error("Report missing")
                            if(editor!=null && editor?.photoUri!=null && editor?.photoUri!=item.photoUri){
                                editor?.photoUri?.let{old->runCatching{contentResolver.delete(Uri.parse(old),null,null)}}
                            }
                            reload();formOpen=false;editor=null;photo=null;proposed=null
                            notice="Observation saved privately on this device.";finished(true)
                        }catch(e:Exception){notice="Could not save report.";finished(false)}
                    }
                },
                onCancel={discardNewPhoto();editor=null;formOpen=false},
                onDelete={item->scope.launch {
                    try {
                        val deleted=withContext(Dispatchers.IO){database.delete(item.id)}
                        if(deleted){
                            item.photoUri?.let{runCatching{contentResolver.delete(Uri.parse(it),null,null)}}
                            reload();notice="Observation removed."
                        }else notice="Report not found."
                    }catch(e:Exception){notice="Could not remove observation."}
                }},
                onAction={item,action,desc,done->scope.launch{
                    val update=item.copy(status=action,actionNote=desc.trim(),actionAt=if(action==ActionStatus.COMPLETED)System.currentTimeMillis() else null,updatedAt=System.currentTimeMillis())
                    val problems=ReportValidation.problems(update)
                    if(problems.isNotEmpty()){notice=problems.first();done(false)}
                    else try{
                        val ok=withContext(Dispatchers.IO){database.update(update)}
                        if(!ok)error("Missing observation")
                        reload();notice="Self-reported action updated. No official verification implied.";done(true)
                    }catch(e:Exception){notice="Could not update action.";done(false)}
                }},
                onExport={
                    pendingCsv=CsvExport.generate(reports.toList()){r->
                        AssessmentEngine.assess(r,reports.toList(),layer?.distanceMeters(r.latitude,r.longitude))
                    }
                    exportLauncher.launch("daluyan-observations.csv")
                },
                onImport={geoLauncher.launch(arrayOf("application/geo+json","application/json","text/plain","*/*"))},
                onSource={startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(OfficialData.SOURCE_URL)))},
                formOpen=formOpen,editing=editor,photo=photo,onCapture=capture
            )
        }
    }
}
