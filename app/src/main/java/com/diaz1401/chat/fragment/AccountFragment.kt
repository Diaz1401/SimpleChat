package com.diaz1401.chat.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Base64
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.diaz1401.chat.R
import com.diaz1401.chat.activities.EditProfileActivity
import com.diaz1401.chat.activities.SignInActivity
import com.diaz1401.chat.database.ProfileDAO
import com.diaz1401.chat.databinding.FragmentAccountBinding
import com.diaz1401.chat.utilities.LocalConstants
import com.diaz1401.chat.utilities.PreferenceManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


class AccountFragment : Fragment(), LocationListener {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var profileDAO: ProfileDAO
    private val firestore = FirebaseFirestore.getInstance()
    private var currentMarker: Marker? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileDAO = ProfileDAO(requireContext())
        preferenceManager = PreferenceManager(requireContext())
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", 0))

        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(GeoPoint(-6.200000, 106.816666)) // Default to Jakarta

        loadFavoritePlaces()

        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(event: MotionEvent) {
                val projection = binding.mapView.projection
                val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt())

                for (overlay in binding.mapView.overlays) {
                    if (overlay is Marker) {
                        val markerPoint = projection.toPixels(overlay.position, null)
                        if (Math.abs(markerPoint.x - event.x) < 50 && Math.abs(markerPoint.y - event.y) < 50) {
                            showDeleteFavoritePlaceDialog(overlay, overlay.id)
                            return
                        }
                    }
                }

                // If no marker was long-pressed, show the add favorite place dialog
                showAddFavoritePlaceDialog(geoPoint as GeoPoint)
            }
        })

        binding.mapView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.btnLogOut.setOnClickListener {
            logOut()
        }

        binding.fabUpdateLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                getLocation()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionCode
                )
            }
        }

        displayProfile()
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        _binding?.let { binding ->
            val currentLocation = GeoPoint(location.latitude, location.longitude)
            binding.mapView.controller.animateTo(currentLocation)
            binding.mapView.controller.setZoom(15.0)

            // You can also add a marker for the current location if desired
            placeMarker(currentLocation, "Current Location")
        }

        // Remove location updates to conserve battery
        locationManager.removeUpdates(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteFavoritePlaceDialog(marker: Marker, documentId: String?) {
        if (documentId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: Invalid document ID", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Favorite Place")
        builder.setMessage("Do you want to delete this favorite place?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            deleteFavoritePlace(marker, documentId)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }


    private fun deleteFavoritePlace(marker: Marker, documentId: String) {
        val userId = preferenceManager.getString(LocalConstants.KEY_USER_ID)
        if (userId.isNullOrEmpty() || documentId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Invalid user ID or document ID", Toast.LENGTH_SHORT).show()
            return
        }
        firestore.collection(LocalConstants.KEY_COLLECTION_USERS)
            .document(userId)
            .collection("favorite_places")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                binding.mapView.overlays.remove(marker)
                binding.mapView.invalidate()
                Toast.makeText(requireContext(), "Place deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to delete place: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showAddFavoritePlaceDialog(geoPoint: GeoPoint) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Favorite Place")

        val input = EditText(requireContext())
        input.hint = "Enter place name"
        builder.setView(input)

        builder.setPositiveButton("Yes") { dialog, _ ->
            val placeName = input.text.toString()
            placeMarker(geoPoint, placeName)
            saveFavoritePlace(placeName, geoPoint.latitude, geoPoint.longitude)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun placeMarker(geoPoint: GeoPoint, title: String) {
        currentMarker?.let { binding.mapView.overlays.remove(it) }
        currentMarker = Marker(binding.mapView).apply {
            position = geoPoint
            this.title = title
            binding.mapView.overlays.add(this)
        }
        binding.mapView.invalidate()
    }

    private fun saveFavoritePlace(title: String, latitude: Double, longitude: Double) {
        val userId = preferenceManager.getString(LocalConstants.KEY_USER_ID) ?: return
        val place = hashMapOf(
            "title" to title,
            "latitude" to latitude,
            "longitude" to longitude
        )

        firestore.collection(LocalConstants.KEY_COLLECTION_USERS)
            .document(userId)
            .collection("favorite_places")
            .add(place)
            .addOnSuccessListener {
                loadFavoritePlaces() // Refresh the map with the new place
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    private fun loadFavoritePlaces() {
        val userId = preferenceManager.getString(LocalConstants.KEY_USER_ID) ?: return
        firestore.collection(LocalConstants.KEY_COLLECTION_USERS)
            .document(userId)
            .collection("favorite_places")
            .get()
            .addOnSuccessListener { documents ->
                binding.mapView.overlays.clear() // Clear existing markers
                for (document in documents) {
                    val latitude = document.getDouble("latitude") ?: continue
                    val longitude = document.getDouble("longitude") ?: continue
                    val title = document.getString("title") ?: "Favorite Place"
                    val marker = Marker(binding.mapView).apply {
                        position = GeoPoint(latitude, longitude)
                        this.title = title
                        this.id = document.id // Set the document ID as the marker's ID
                    }
                    binding.mapView.overlays.add(marker)
                }
                binding.mapView.invalidate()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load places: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun decodeImage(base64Str: String?): Bitmap? {
        return if (!base64Str.isNullOrEmpty()) {
            try {
                val byteArray = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun logOut() {
        showToast("Signing out...")
        val database = FirebaseFirestore.getInstance()
        val documentReference =
            database.collection(LocalConstants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(LocalConstants.KEY_USER_ID)!!
            )
        val updates = HashMap<String, Any>()
        updates[LocalConstants.KEY_FCM_TOKEN] = FieldValue.delete()
        documentReference.update(updates)
            .addOnSuccessListener {
                preferenceManager.clear()
                startActivity(Intent(requireContext(), SignInActivity::class.java))
                requireActivity().finish()
            }
            .addOnFailureListener {
                showToast(
                    "Unable to sign out"
                )
            }
    }

    private fun displayProfile() {
        val name = preferenceManager.getString(LocalConstants.KEY_NAME)
        val email = preferenceManager.getString(LocalConstants.KEY_EMAIL)
        val image = preferenceManager.getString(LocalConstants.KEY_IMAGE)
        val id = preferenceManager.getString(LocalConstants.KEY_USER_ID)
        val bio = preferenceManager.getString(LocalConstants.KEY_BIO)

        binding.txtName.text = name
        binding.txtEmail.text = email
        binding.txtId.text = id
        if (bio.isNullOrEmpty()) {
            binding.txtBIO.text = getString(R.string.bio_not_available)
        } else {
            binding.txtBIO.text = bio
        }
        val bitmap = decodeImage(image)
        if (bitmap != null) {
            binding.imgProfile.setImageBitmap(bitmap)
        } else {
            binding.imgProfile.setImageResource(R.drawable.person) // Use a default image
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        _binding = null
    }
}