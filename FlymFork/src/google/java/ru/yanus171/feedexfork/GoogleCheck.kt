package ru.yanus171.feedexfork

import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller


object GoogleCheck {
    fun check(){
        try {
            ProviderInstaller.installIfNeeded(MainApplication.getContext())
            //Toast.makeText( mContext,  "ProviderInstaller.installIfNeeded", Toast.LENGTH_LONG  ).show();
        } catch (e: GooglePlayServicesRepairableException) {
            //Toast.makeText( mContext,  "GooglePlayServicesRepairableException", Toast.LENGTH_LONG  ).show();
            GoogleApiAvailability.getInstance().showErrorNotification(MainApplication.getContext(), e.getConnectionStatusCode())
        } catch (e: GooglePlayServicesNotAvailableException) {
            //Toast.makeText( mContext,  "GooglePlayServicesNotAvailableException", Toast.LENGTH_LONG  ).show();
        }
    }
}