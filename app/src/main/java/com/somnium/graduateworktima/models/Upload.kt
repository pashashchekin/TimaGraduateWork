package com.somnium.graduateworktima.models

open class Upload {

    var imageUrl: String
    var name : String
    var key : String


    constructor():this("","", "")


    constructor(imageUrl: String,name: String, key: String) {
        this.imageUrl = imageUrl
        this.name = name
        this.key = key
    }


}