package com.example.map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.map.databinding.ActivityMainBinding;

import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;


public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();

    private final int REQUEST_LOCATION_CODE = 1000;

    private boolean trackingModeCheck = true;

    private ActivityMainBinding binding;

    private ConnectivityManager connectivityManager;
    private LocationManager locationManager;
    private ViewGroup mapViewContainer;
    private MapView mapView;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
        }

        connectivityManager = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(getBestProvider(), 1000, 0, currentLocationTracker);

        mapView = new MapView(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);

        mapViewContainer = binding.mapView;
        mapViewContainer.addView(mapView);

        mapView.setMapViewEventListener(mapViewEventListener);

        binding.btnCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                moveCurrentLocation();
            }
        });

        binding.btnLocationTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackingModeCheck = binding.btnLocationTracker.isChecked();
            }
        });
    }

    private void moveCurrentLocation() {
        @SuppressLint("MissingPermission") Location lastKnownLocation = locationManager.getLastKnownLocation(getBestProvider());
//        @SuppressLint("MissingPermission") Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        @SuppressLint("MissingPermission") Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//        @SuppressLint("MissingPermission") Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mapView.setMapCenterPoint(mapPoint, true);
        MapReverseGeoCoder reverseGeoCoder = new MapReverseGeoCoder(getString(R.string.kakao_app_key), mapPoint, reverseGeoCodingResultListener, MainActivity.this);
        reverseGeoCoder.startFindingAddress();
    }

    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);// 전원 소비량
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);// 고도, 높이 값을 얻어 올지를 결정
        criteria.setAltitudeRequired(false);// provider 기본 정보(방위, 방향)
        criteria.setBearingRequired(false);// 속도
        criteria.setSpeedRequired(false);// 위치 정보를 얻어 오는데 들어가는 금전적 비용
        criteria.setCostAllowed(true);
        return criteria;
    }

    private String getBestProvider() {
        return locationManager.getBestProvider(getCriteria(), true);
    }

    private LocationListener currentLocationTracker = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d("동환", "onLocationChanged");
            Log.d("동환", "LastKnownLocation : " + locationManager.getLastKnownLocation(getBestProvider()));
            if (trackingModeCheck) {
                moveCurrentLocation();
            }

//            Log.d("동환", "LastKnownLocation : " + locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
//            Log.d("동환", "LastKnownLocation : " + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
//            Log.d("동환", "LastKnownLocation : " + locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("동환", "onStatusChanged");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d("동환", "onProviderEnabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d("동환", "onProviderDisabled");
        }
    };

    private MapReverseGeoCoder.ReverseGeoCodingResultListener reverseGeoCodingResultListener = new MapReverseGeoCoder.ReverseGeoCodingResultListener() {
        @Override
        public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
            binding.tvAddress.setText(s);
        }

        @Override
        public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
            binding.tvAddress.setText("주소를 찾을 수 없습니다.");
            Toast.makeText(MainActivity.this, "문제가 발생하여 앱을 종료합니다.\n네트워크 연결 상태를 확인하세요", Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private MapView.MapViewEventListener mapViewEventListener = new MapView.MapViewEventListener() {
        @Override
        public void onMapViewInitialized(MapView mapView) {
            Log.d("동환", "onMapViewInitialized");
        }

        @Override
        public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewCenterPointMoved");
            Glide.with(MainActivity.this).asGif().load(R.drawable.loading).into(binding.ivState);
        }

        @Override
        public void onMapViewZoomLevelChanged(MapView mapView, int i) {
            Log.d("동환", "onMapViewZoomLevelChanged");
        }

        @Override
        public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewSingleTapped");
        }

        @Override
        public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewDoubleTapped");
        }

        @Override
        public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewLongPressed");
        }

        @Override
        public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewDragStarted");
        }

        @Override
        public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewDragEnded");
        }

        @Override
        public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
            Log.d("동환", "onMapViewMoveFinished");
            Glide.with(MainActivity.this).load(R.drawable.image_current_location).into(binding.ivState);
            MapReverseGeoCoder reverseGeoCoder = new MapReverseGeoCoder(getString(R.string.kakao_app_key), mapPoint, reverseGeoCodingResultListener, MainActivity.this);
            reverseGeoCoder.startFindingAddress();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
                    localBuilder.setTitle("권한 설정")
                            .setMessage("꼭 필요한 권한입니다.\n설정하지 않으면 앱을 사용할 수 없습니다.")
                            .setPositiveButton("권한 설정하러 가기", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.parse("package:" + getPackageName()));
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .setNegativeButton("취소하기", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                                    finish();
                                }
                            })
                            .create()
                            .show();
                }
                break;
            }
        }
    }

}

