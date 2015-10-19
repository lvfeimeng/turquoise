# Turquoise 1.4.151019

## Description
> Android Library<br/>
> 1.Configurable "Application"/"Activity"... | 可配置的两大组件<br/>
> 2.SlideEngine | 滑动引擎<br/>
> 3.BitmapLoader | 双缓存图片异步加载器<br/>
> 4.some Utils | 工具<br/>
> https://github.com/shepherdviolet/turquoise <br/>

## Modules
> library.turquoise : the "Turquoise" library module  |  库本体 <br/>
> library.demoa : demos of "Turquoise" library  |  示例程序 <br/>
> library.recycler : some messy code. IGNORE PLEASE  |  未整理的凌乱代码, 请无视 <br/>

## Export & Use *.aar
>1.build library <br/>
>2.get library.turquoise/build/outputs/library.turquoise-release.aar <br/>
>3.put .aar into your module (modulename/libs/) <br/>
>4.edit build.gradle <br/>

```java
repositories {
    ......
    flatDir {
        dirs 'libs'
    }
}
```

```java
dependencies {
    ......
    compile(name:'library.turquoise-release', ext:'aar')
}
```

## Releases
>https://github.com/shepherdviolet/turquoise/blob/master/aar/sviolet.turquoise-1.3.151016-rc.aar<br/>
>https://github.com/shepherdviolet/turquoise/blob/master/aar/sviolet.turquoise-1.3.151019-beta1.aar<br/>