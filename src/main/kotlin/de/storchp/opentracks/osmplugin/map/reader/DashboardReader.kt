package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.model.TrackStatistics
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.map.model.TrackpointsDebug
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import org.oscim.core.GeoPoint

private val TAG: String = DashboardReader::class.java.getSimpleName()

private const val EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION"
private const val EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK =
    "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK"
private const val EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON"
private const val EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON"
private const val EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN"

typealias UpdateTrackStatistics = (TrackStatistics?) -> Unit
typealias UpdateTrackpointsDebug = (TrackpointsDebug) -> Unit

class DashboardReader(
    intent: Intent,
    private val contentResolver: ContentResolver,
    mapData: MapData,
    updateTrackStatistics: UpdateTrackStatistics,
    updateTrackpointsDebug: UpdateTrackpointsDebug,
) : MapDataReader(mapData, updateTrackStatistics, updateTrackpointsDebug) {

    private var trackpointsDebug = TrackpointsDebug()
    private var lastWaypointId: Long? = null
    private var lastTrackPointId: Long? = null
    private var contentObserver: OpenTracksContentObserver? = null
    val tracksUri: Uri
    val trackpointsUri: Uri
    val waypointsUri: Uri?
    val protocolVersion: Int

    init {
        require(intent.isDashboardAction())
        val uris =
            intent.getParcelableArrayListExtra<Uri>(APIConstants.ACTION_DASHBOARD_PAYLOAD)!!
        protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1)
        tracksUri = APIConstants.getTracksUri(uris)
        trackpointsUri = APIConstants.getTrackpointsUri(uris)
        waypointsUri = APIConstants.getWaypointsUri(uris)
        keepScreenOn = intent.getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false)
        showOnLockScreen = intent.getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false)
        showFullscreen = intent.getBooleanExtra(EXTRAS_SHOW_FULLSCREEN, false)
        isRecording = intent.getBooleanExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, false)

        trackpointsDebug.protocolVersion = protocolVersion
        readTrackpoints(trackpointsUri, false, protocolVersion)
        readTracks(tracksUri)
        waypointsUri?.let { readWaypoints(it) }
    }

    fun readTrackpoints(data: Uri, update: Boolean, protocolVersion: Int) {
        Log.i(TAG, "Loading trackpoints from $data")
        val trackpointsBySegments: TrackpointsBySegments =
            TrackpointReader.readTrackpointsBySegments(
                contentResolver,
                data,
                lastTrackPointId,
                protocolVersion
            )
        if (trackpointsBySegments.isEmpty()) {
            Log.d(TAG, "No new trackpoints received")
            return
        }
        if (trackpointsBySegments.isNotEmpty() && trackpointsBySegments.last().isNotEmpty()) {
            lastTrackPointId = trackpointsBySegments.last().last().id
        }
        readTrackpoints(trackpointsBySegments, update, isRecording)
    }

    private fun readWaypoints(data: Uri) {
        // Sanitize and parameterize the input to prevent SQL Injection
        val selection = "waypoint_data = ?"  // Example query condition
        val selectionArgs = arrayOf(data.toString())  // Sanitize and pass as parameter

        // Perform the query using parameterized selectionArgs
        val cursor = waypointsUri?.let {
            contentResolver.query(
                it,  // URI for the data
                null,          // Columns you want to retrieve (use null for all columns)
                selection,     // Query condition (parameterized)
                selectionArgs, // The sanitized data
                null            // Sort order
            )
        }

        // Check if the cursor is null or empty
        if (cursor == null || !cursor.moveToFirst()) {
            Log.e(TAG, "Cursor is null or no data found")
            return
        }

        val waypoints = mutableListOf<Waypoint>()

        cursor.let {
            do {
                // Get column indexes to avoid repeated calls
                val idColumnIndex = it.getColumnIndex("id")
                val nameColumnIndex = it.getColumnIndex("name")
                val latLongColumnIndex = it.getColumnIndex("latLong")  // Assuming 'latLong' is a column in the database

                // Check if the columns exist before accessing them
                if (idColumnIndex != -1 && nameColumnIndex != -1 && latLongColumnIndex != -1) {
                    val latLongString = it.getString(latLongColumnIndex) // Get latLong as string (e.g., "lat,long")

                    // Handle malformed latLong format (if any)
                    val latLongParts = latLongString.split(",")
                    if (latLongParts.size == 2) {
                        // Successfully split latitude and longitude
                        val latitude = latLongParts[0].toDoubleOrNull() ?: 0.0  // Convert latitude
                        val longitude = latLongParts[1].toDoubleOrNull() ?: 0.0  // Convert longitude

                        // Create GeoPoint from latitude and longitude
                        val geoPoint = GeoPoint(latitude, longitude)

                        val waypoint = Waypoint(
                            id = it.getLong(idColumnIndex),
                            name = it.getString(nameColumnIndex),
                            latLong = geoPoint  // Pass the GeoPoint object directly
                        )
                        waypoints.add(waypoint)
                    } else {
                        // Log error and skip this entry if latLong format is invalid
                        Log.e(TAG, "Invalid latLong format: $latLongString")
                    }
                } else {
                    Log.e(TAG, "Column(s) missing in the database query result")
                }
            } while (it.moveToNext())

            it.close()
        }

        if (waypoints.isNotEmpty()) {
            lastWaypointId = waypoints.last().id
        }

        // Continue processing the waypoints list
        readWaypoints(waypoints)
    }







    private fun readTracks(data: Uri) {
        readTracks(TrackReader.readTracks(contentResolver, data))
    }

    override fun startContentObserver() {
        contentObserver = OpenTracksContentObserver(
            tracksUri,
            trackpointsUri,
            waypointsUri,
            protocolVersion
        )

        contentResolver.registerContentObserver(tracksUri, false, contentObserver!!)
        contentResolver.registerContentObserver(
            trackpointsUri,
            false,
            contentObserver!!
        )
        if (waypointsUri != null) {
            contentResolver.registerContentObserver(
                waypointsUri,
                false,
                contentObserver!!
            )
        }

    }

    override fun unregisterContentObserver() {
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        contentObserver = null
    }

    private inner class OpenTracksContentObserver(
        private val tracksUri: Uri,
        private val trackpointsUri: Uri,
        private val waypointsUri: Uri?,
        private val protocolVersion: Int
    ) : ContentObserver(Handler(Looper.getMainLooper())) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) {
                return  // nothing can be done without an uri
            }
            if (tracksUri.toString().startsWith(uri.toString())) {
                readTracks(tracksUri)
            } else if (trackpointsUri.toString().startsWith(uri.toString())) {
                readTrackpoints(trackpointsUri, true, protocolVersion)
            } else if (waypointsUri?.toString()?.startsWith(uri.toString()) == true) {
                readWaypoints(waypointsUri)
            }
        }
    }

}

fun Intent.isDashboardAction(): Boolean {
    return APIConstants.ACTION_DASHBOARD == action
}
