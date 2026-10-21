package ph.org.daluyan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import ph.org.daluyan.domain.SiteReport
import androidx.compose.ui.unit.dp

@Composable
fun MapPanel(reports: List<SiteReport>, selected: Pair<Double,Double>?, onPick:(Double,Double)->Unit) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    val onPickLatest by rememberUpdatedState(onPick)
    val view=remember(context){MapView(context).apply{onCreate(null)}}
    var map by remember{mutableStateOf<MapLibreMap?>(null)}
    val markers=remember{mutableListOf<Marker>()}
    DisposableEffect(lifecycle,view){
        val observer=LifecycleEventObserver{_,event-> when(event){
            Lifecycle.Event.ON_START->view.onStart()
            Lifecycle.Event.ON_RESUME->view.onResume()
            Lifecycle.Event.ON_PAUSE->view.onPause()
            Lifecycle.Event.ON_STOP->view.onStop()
            else->Unit
        }}
        lifecycle.lifecycle.addObserver(observer)
        if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))view.onStart()
        if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))view.onResume()
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))view.onPause()
            if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))view.onStop()
            view.onDestroy()
        }
    }
    LaunchedEffect(view){
        view.getMapAsync{ready->
            ready.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) {
                ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(13.390,121.180),10.0))
                ready.addOnMapClickListener(MapLibreMap.OnMapClickListener { pos-> onPickLatest(pos.latitude,pos.longitude);true })
                map=ready
            }
        }
    }
    LaunchedEffect(map,selected){selected?.let {map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.first,it.second),13.0))}}
    LaunchedEffect(map,reports,selected) {
        map?.let{ready->
            markers.forEach{ready.removeMarker(it)};markers.clear()
            reports.forEach{record->markers.add(ready.addMarker(MarkerOptions().position(LatLng(record.latitude,record.longitude)).title(record.title).snippet("Community report · not verified")))}
            selected?.let{markers.add(ready.addMarker(MarkerOptions().position(LatLng(it.first,it.second)).title("Selected place")))}
        }
    }
    AndroidView(factory={view},modifier=Modifier.fillMaxWidth().height(350.dp))
}
