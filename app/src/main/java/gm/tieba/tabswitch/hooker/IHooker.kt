package gm.tieba.tabswitch.hooker

interface IHooker {

    fun key(): String

    @Throws(Throwable::class)
    fun hook()
}
