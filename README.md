# VlcPlayer

#### 介绍
基于libvlc aar的android播放器实例

#### 使用说明

修改app的build.gradle文件，来选择用so还是用aar来编译apk

//implementation(name: 'libvlc-all-3.3.0-eap17', ext: 'aar')
implementation project(path: ':libvlc')

