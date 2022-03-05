package com.timmahh.sleepasha.util


fun Context.isNetworkConnected() {
    (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo

}