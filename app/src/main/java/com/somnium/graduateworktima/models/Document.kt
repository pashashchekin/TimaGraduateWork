package com.somnium.graduateworktima.models

open class Document {
    var key : String
    var name : String
    var url: String
    var date: String

    constructor():this("","", "","")


    constructor(key: String, name: String, url: String, date: String ) {
        this.key = key
        this.name = name
        this.url = url
        this.date = date
    }


}