package com.example.caps_project1;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private FragmentActivity mContext;
    private Context context;

    public MarkerOptions markerOptions;

    private static final String TAG = MapActivity.class.getSimpleName();
    private GoogleMap mMap;
    private MapView mapView = null;
    private Marker currentMarker = null;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient; // Deprecated??? FusedLocationApi??? ??????
    private LocationRequest locationRequest;
    private Location mCurrentLocation;

    private final LatLng mDefaultLocation = new LatLng(37.56, 126.97);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000 * 60 * 1;  // 1??? ?????? ?????? ??????
    private static final int FASTEST_UPDATE_INTERVAL_MS = 1000 * 30; // 30??? ????????? ?????? ??????

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    public MapActivity() {
    }

    @Override
    public void onAttach(Activity activity) { // Fragment ??? Activity??? attach ??? ??? ????????????.
        mContext = (FragmentActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    //  ????????????
    //  ????????????

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Layout ??? inflate ?????? ?????????.
        if (savedInstanceState != null) {
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            CameraPosition mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        View layout = inflater.inflate(R.layout.activity_map, container, false);
        mapView = (MapView) layout.findViewById(R.id.map);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }
        mapView.getMapAsync(this);
/*
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.activity_map, container, false);

        ImageButton hospital = viewGroup.findViewById(R.id.hospital);
        hospital.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PharmParser_object pharmParser_object = new PharmParser_object("hospital", "?????????");

                ArrayList<PharmDTO_object> pharmDTO_objects_hospital = pharmParser_object.FileOpen();

                for (PharmDTO_object location : pharmDTO_objects_hospital) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    markerOptions.position(latLng);

                    mMap.addMarker(markerOptions);
                }
            }
        });
*/
        return layout;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // Fragement????????? OnCreateView??? ?????????, Activity?????? onCreate()??? ???????????? ?????? ???????????? ???????????????.
        // Activity??? Fragment??? ?????? ?????? ????????? ?????????, View??? ???????????? ????????? ????????? ?????????.
        super.onActivityCreated(savedInstanceState);

        //??????????????? ?????? ????????? ??? ???????????? ??????
        MapsInitializer.initialize(mContext);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // ???????????? ?????????????????? ??????
                .setInterval(UPDATE_INTERVAL_MS) // ????????? Update ?????? ??????
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS); // ?????? ????????? ?????????????????? ??????

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        // FusedLocationProviderClient ?????? ??????
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                Log.d(TAG, "Loading");
                setDefaultLocation(); // GPS??? ?????? ????????? ????????? ?????? ?????? ????????? ?????? ????????? ?????????.

                getLocationPermission();

                updateLocationUI();

                getDeviceLocation();
            }
        });


        MarkerOptions marker = new MarkerOptions();
        // ???????????????
//        marker.position(new LatLng(37.3215984, 127.1267503))
//                .title("??????????????? ???????????????")
//                .snippet("????????? ????????? ????????? ????????? 152");
        // ??????????????????
        marker.position(new LatLng(37.3041347, 127.1061582))
                .title("??????????????????")
                .snippet("????????? ????????? ????????? ????????? 700-6??????");
        mMap.addMarker(marker).showInfoWindow();

        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Snackbar.make(mapView, marker.getTitle() + "    ??? 1899 - 3700", Snackbar.LENGTH_INDEFINITE).show();
        Snackbar.make(mapView, marker.getTitle() + "    ??? 031-287-7275", Snackbar.LENGTH_INDEFINITE).show();
        return false;
    }
/*
    private Marker addMarker(PharmDTO_object pharmDTO_object) {
        LatLng location = new LatLng(pharmDTO_object.getLatitude(), pharmDTO_object.getLongitude());
        String locality = pharmDTO_object.getLocality();
        String name = pharmDTO_object.getName();
        String address = pharmDTO_object.getAddress();
        String number = pharmDTO_object.getNumber();
        String state = pharmDTO_object.getState();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(locality);
        markerOptions.title(name);
        markerOptions.title(address);
        markerOptions.title(number);
        markerOptions.title(state);
        markerOptions.position(location);

        return mMap.addMarker(markerOptions);
    } */
/*
    public static Location addrToPoint(Context context, String address) {
        Location location = new Location("");
        List<Address> addresses = null;

        if(addresses != null) {
            for(int i=0; i<addresses.size();i++) {
                Address lating = addresses.get(i);
                location.setLatitude(lating.getLatitude());
                location.setLongitude(lating.getLongitude());
            }
        }
        return location;
    }
*/
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mCurrentLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void setDefaultLocation() {
        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(mDefaultLocation);
        markerOptions.title("???????????? ????????? ??? ??????");
        markerOptions.snippet("?????? ???????????? GPS ?????? ?????? ???????????????");
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mMap.addMarker(markerOptions);
    }

    String getCurrentAddress(LatLng latlng) {
        // ?????? ????????? ?????????????????? ?????? ???????????? ?????????.
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

        // ??????????????? ???????????? ?????? ???????????? ?????????.
        try {
            addressList = geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1);
        } catch (IOException e) {
            Toast.makeText(mContext, "??????????????? ????????? ????????? ??? ????????????. ??????????????? ???????????? ????????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return "?????? ?????? ??????";
        }

        if (addressList.size() < 1) { // ?????? ???????????? ??????????????? ?????? ?????????
            return "?????? ????????? ?????? ??????";
        }

        // ????????? ?????? ???????????? ???????????? ??????
        Address address = addressList.get(0);
        StringBuilder addressStringBuilder = new StringBuilder();
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            addressStringBuilder.append(address.getAddressLine(i));
            if (i < address.getMaxAddressLineIndex())
                addressStringBuilder.append("\n");
        }

        return addressStringBuilder.toString();
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                Location location = locationList.get(locationList.size() - 1);

                LatLng currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());

                String markerTitle = getCurrentAddress(currentPosition);
                String markerSnippet = "??????:" + String.valueOf(location.getLatitude())
                        + " ??????:" + String.valueOf(location.getLongitude());

                Log.d(TAG, "Time :" + CurrentTime() + " onLocationResult : " + markerSnippet);

                //?????? ????????? ?????? ???????????? ??????
                setCurrentLocation(location, markerTitle, markerSnippet);
                mCurrentLocation = location;
            }
        }

    };

    private String CurrentTime() {
        Date today = new Date();
        SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat time = new SimpleDateFormat("hh:mm:ss a");
        return time.format(today);
    }

    //???????????? ????????????
    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        if (currentMarker != null) currentMarker.remove();

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);
    }


    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(mContext,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onStart() { // ???????????? Fragment??? ???????????? ?????????.
        super.onStart();
        mapView.onStart();
        Log.d(TAG, "onStart ");
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        if (mFusedLocationProviderClient != null) {
            Log.d(TAG, "onStop : removeLocationUpdates");
            mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onResume() { // ???????????? Fragment??? ????????????, ????????? ??????????????? ???????????? ?????? ??????
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() { // ?????????????????? ????????? View ??? ???????????? ??????
        super.onDestroyView();
        if (mFusedLocationProviderClient != null) {
            Log.d(TAG, "onDestroyView : removeLocationUpdates");
            mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onDestroy() {
        // Destroy ??? ??????, ????????? OnDestroyView?????? View??? ????????????, OnDestroy()??? ????????????.
        super.onDestroy();
        mapView.onDestroy();
    }

}