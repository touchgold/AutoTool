package com.tanjinc.autotool

data class Person(var age:Int?, var name:String, var email:String = "123@cmcm.com") {
    var cardList: MutableList<String> = mutableListOf()
}

fun sum(a:Int, b: Int) = a + b


//参数判空
fun printEmail(person: Person?) {
    println(person?.email)
}


fun printCard(person: Person?) {
    println(person?.cardList)

    if (person?.email.isNullOrEmpty()) {
    }
    person?.email.isNullOrEmpty()
}

fun getAge(person: Person?):Int? {
    return person?.age ?: -1
}


fun main(args: Array<String>) {

    val person = Person(18,"aa")

    person.cardList = mutableListOf("1", "2", "3")
    person.cardList.add(0, "6")

    println(person.email)
    println(sum(2,9))

    //字符串模板
    val i = 100
    val j = "i is $i"
    print(j)

    printEmail(null)
    printCard(person)


    var map: MutableMap<String, Any> = mutableMapOf()
    map = mutableMapOf()
    map.put("email", "123@cmcm.com")
    map.put("age", 18)

    //或者
    map["name"] = "cmcm"

    for((k, v) in map) {
        if (map[k] == "age") {
            map.remove(k)
        }
        println("$k -> $v")
    }

    args.flatMap {
        it.split("_")
    }.map {
        print("$it")
    }

    val regex = Regex("[0-9]+评")
    val regex2 = Regex("视频")
    val str1 = "你怎么评价这几位主演呢？欢迎在下方评论留言！"
    val str2 = "视频"
    print(str1.contains(regex))
    print("\n")
    print(str2.matches(regex2))
}