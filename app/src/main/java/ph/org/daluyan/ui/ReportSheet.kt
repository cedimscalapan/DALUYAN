package ph.org.daluyan.ui

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ph.org.daluyan.domain.*
import java.util.Locale

@Composable
fun ReportSheet(
    existing:SiteReport?,initial:Pair<Double,Double>?,source:LocationSource,accuracy:Float?,
    photo:Uri?,onCapture:()->Unit,onCancel:()->Unit,onSave:(SiteReport,(Boolean)->Unit)->Unit
) {
    var step by rememberSaveable(existing?.id){mutableIntStateOf(0)}
    var title by rememberSaveable(existing?.id){mutableStateOf(existing?.title?:"")}
    var lat by rememberSaveable(existing?.id){mutableStateOf(existing?.latitude?.toString()?:initial?.first?.toString()?:"")}
    var lon by rememberSaveable(existing?.id){mutableStateOf(existing?.longitude?.toString()?:initial?.second?.toString()?:"")}
    var level by rememberSaveable(existing?.id){mutableIntStateOf(existing?.severity?:2)}
    var access by remember(existing?.id){mutableStateOf(existing?.access?:SiteAccess.PUBLIC)}
    var notes by rememberSaveable(existing?.id){mutableStateOf(existing?.notes?:"")}
    var error by remember{mutableStateOf("")}
    var saving by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
            .padding(start=20.dp,end=20.dp,top=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Eyebrow(if(existing==null)"New field observation" else "Edit field observation")
        BigTitle(if(existing==null)"Record what you see." else "Correct the record.")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            (0..2).forEach {n->Surface(color=if(step==n)Forest else Soft,shape=RoundedCornerShape(20.dp),
                modifier=Modifier.weight(1f).height(5.dp)){} }
        }
        Muted(when(step){0->"Step 1 of 3 · Where";1->"Step 2 of 3 · What";else->"Step 3 of 3 · Review"})
        when(step){
            0->{
                Text("Where is the site?",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Muted("Your selected map point fills the coordinates. Correct them if needed.")
                Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(lat,{lat=it.take(24)},label={Text("Latitude")},singleLine=true,
                        modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                    OutlinedTextField(lon,{lon=it.take(24)},label={Text("Longitude")},singleLine=true,
                        modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                }
                OutlinedTextField(title,{title=it.take(80)},label={Text("Place name (optional)")},
                    singleLine=true,modifier=Modifier.fillMaxWidth(),supportingText={Text("A short name makes reports easier to find.")})
                if(source==LocationSource.GPS && existing==null)Muted("GPS accuracy ${accuracy?.toInt()?.let { "$it m (approx.)" }?:"unknown"}. Confirm the location before saving.")
            }
            1->{
                Text("Describe the observation",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Muted("Choose the visible litter level. A photograph improves evidence, but is optional.")
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    listOf(Triple(1,"Low","Few visible items"),Triple(2,"Moderate","Several visible items"),Triple(3,"High","Concentrated or widespread litter")).forEach{(n,label,explanation)->
                        Surface(color=if(level==n)Soft else androidx.compose.ui.graphics.Color.White,shape=RoundedCornerShape(14.dp),
                            modifier=Modifier.fillMaxWidth().clickable{level=n}) {
                            Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
                                RadioButton(selected=level==n,onClick={level=n})
                                Spacer(Modifier.width(7.dp))
                                Column {Text(label,fontWeight=FontWeight.SemiBold);Muted(explanation)}
                            }
                        }
                    }
                }
                OutlinedButton(onClick=onCapture,modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt,null);Spacer(Modifier.width(8.dp));Text(if(photo==null)"Attach evidence photo" else "Retake evidence photo")
                }
                photo?.let{picture->
                    AndroidView(factory={ImageView(it).apply{scaleType=ImageView.ScaleType.CENTER_CROP}},
                        update={it.setImageURI(picture)},modifier=Modifier.fillMaxWidth().height(155.dp))
                    Muted("Photo stored privately on this device; never shared automatically.")
                }
            }
            else->{
                Text("Review and save",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Text("Site access",fontWeight=FontWeight.SemiBold)
                SiteAccess.entries.forEach{s->
                    Surface(color=if(access==s)Soft else androidx.compose.ui.graphics.Color.White,shape=RoundedCornerShape(13.dp),
                        modifier=Modifier.fillMaxWidth().clickable{access=s}){
                        Row(Modifier.padding(9.dp),verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(selected=access==s,onClick={access=s})
                            Text(s.title)
                        }
                    }
                }
                if(access!=SiteAccess.PUBLIC)Text("Do not enter restricted or unsafe locations.",color=MaterialTheme.colorScheme.error)
                OutlinedTextField(notes,{notes=it.take(500)},label={Text("Additional notes (optional)")},
                    minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth())
                Panel {
                    Text("Before saving",fontWeight=FontWeight.Bold)
                    Muted("This is a personal, unverified observation. GIS proximity and priority are calculated separately if trusted map data is imported.")
                    Text("No report is sent to an LGU or public service.",fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
        Surface(color=Mist,shadowElevation=6.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),
            horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedButton(onClick={if(step==0){onCancel()}else{step--;error=""}},enabled=!saving,modifier=Modifier.weight(1f)) {
                Text(if(step==0)"Cancel" else "Back")
            }
            Button(onClick={
                if(step==0){
                    val a=lat.toDoubleOrNull();val b=lon.toDoubleOrNull()
                    error=if(a==null||b==null||!a.isFinite()||!b.isFinite()||a !in -90.0..90.0||b !in -180.0..180.0)
                        "Enter valid latitude and longitude." else ""
                    if(error.isEmpty())step++
                }else if(step==1){step++;error=""}
                else {
                    val a=lat.toDoubleOrNull()?:Double.NaN;val b=lon.toDoubleOrNull()?:Double.NaN
                    val actualName=title.trim().ifEmpty{"Site ${"%.4f".format(Locale.US,a)}, ${"%.4f".format(Locale.US,b)}"}
                    val r=existing?.copy(title=actualName,latitude=a,longitude=b,severity=level,access=access,
                        notes=notes.trim(),photoUri=photo?.toString(),updatedAt=System.currentTimeMillis(),
                        locationSource=if(a!=existing.latitude || b!=existing.longitude)LocationSource.MANUAL else existing.locationSource,
                        accuracyMeters=if(a!=existing.latitude || b!=existing.longitude)null else existing.accuracyMeters)
                        ?: SiteReport(title=actualName,latitude=a,longitude=b,severity=level,access=access,
                            notes=notes.trim(),photoUri=photo?.toString(),locationSource=source,accuracyMeters=if(source==LocationSource.GPS)accuracy else null)
                    val problems=ReportValidation.problems(r)
                    error=problems.firstOrNull()?:""
                    if(error.isEmpty()){
                        saving=true
                        onSave(r){ok->saving=false;if(!ok)error="Unable to save. Your draft is still open."}
                    }
                }
            },enabled=!saving,modifier=Modifier.weight(1f)){
                Text(when{saving->"Saving…";step==2->"Save report";else->"Continue"})
            }
        }
        }
    }
}