package com.example.badeapp.api

data class HavvarselFrostDto(val data: FrostData)

data class FrostData(val tseries: List<Tseries>)

data class Tseries(val header: Header)

data class Header(val extra: Extra)

data class Extra(val name: String, val pos: Pos)

data class Pos(val lon: String, val lat: String)