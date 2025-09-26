package com.bird2fish.birdtalksdk.uihelper

import com.bird2fish.birdtalksdk.model.User

// 用户信息的类型转换工具
object  UserHelper {
    fun pbUserInfo2LocalUser(ui:com.bird2fish.birdtalksdk.pbmodel.User.UserInfo): com.bird2fish.birdtalksdk.model.User{
        val localUser = com.bird2fish.birdtalksdk.model.User()
        localUser.id = ui.userId
        localUser.nick = ui.nickName
        localUser.name = ui.userName
        localUser.age = ui.age
        localUser.gender = ui.gender
        localUser.icon = ui.icon
        localUser.email = ui.email
        localUser.phone = ui.phone
        localUser.introduction = ui.intro

        return localUser
    }
}