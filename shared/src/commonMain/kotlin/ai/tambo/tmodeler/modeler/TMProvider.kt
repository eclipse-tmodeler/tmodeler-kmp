package ai.tambo.tmodeler.modeler

interface TMProvider {
    fun onServerStatusChange(isConnected: Boolean)
    fun onCommit()
}