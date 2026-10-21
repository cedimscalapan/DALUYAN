package ph.org.daluyan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import ph.org.daluyan.domain.SiteReport

/** Online OSM-based OpenFreeMap map; offline satellite/tiles are not provided. */
@Composable
fun MapPanel(reports: List<SiteReport>, selected: Pair<Double, Double>?,
             onPick: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onPick)
    val view = remember(context) { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val markers = remember { mutableListOf<Marker>() }

    DisposableEffect(view, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStart()
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onPause()
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStop()
            view.onDestroy()
        }
    }
    LaunchedEffect(view) {
        view.getMapAsync { ready ->
            ready.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) {
                ready.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(13.390,121.180),10.0))
                ready.addOnMapClickListener(MapLibreMap.OnMapClickListener { point ->
                    callback(point.latitude,point.longitude)
                    true
                })
                map = ready
            }
        }
    }
    LaunchedEffect(map, selected) {
        selected?.let { (latitude,longitude) ->
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude,longitude),13.5))
        }
    }
    LaunchedEffect(map, reports, selected) {
        map?.let { ready ->
            markers.forEach { ready.removeMarker(it) }
            markers.clear()
            reports.forEach { report ->
                markers.add(ready.addMarker(MarkerOptions()
                    .position(LatLng(report.latitude,report.longitude))
                    .title(report.title).snippet("Unverified community observation")))
            }
            selected?.let { (lat,lon) ->
                markers.add(ready.addMarker(MarkerOptions().position(LatLng(lat,lon)).title("Selected location")))
            }
        }
    }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().height(330.dp))
}
