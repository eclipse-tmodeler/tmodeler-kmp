package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.field.advanced.TimeField
import ai.tambo.tmodeler.field.common.IntField
import ai.tambo.tmodeler.ms.TmsGb

class Tstate : TModel0<Tstate>() {

    override fun instance(): Tstate {
        return Tstate()
    }

    // propriétés
    var created = TimeField()
    var uploaded = TimeField()
    var permission = TimeField()
    var granted = TimeField()
    var denied = TimeField()
    var loaded = TimeField()
    var started = TimeField()
    var ended = TimeField()
    var canceled = TimeField()
    var stopped = TimeField()
    var enabled = TimeField()
    var disabled = TimeField()
    var connected = TimeField()
    var disconnected = TimeField()
    var updated = TimeField()
    var done = TimeField()
    var deleted = TimeField()
    var response = TimeField()

    var sent = TimeField()
    var received = TimeField()
    var read = TimeField()

    var status = IntField()

    // ----------------------------
    companion object {
        object Status {
            const val CREATED = 0
            const val UPLOADED = 1
            const val PERMISSION = 2
            const val GRANTED = 3
            const val DENIED = 4
            const val LOADED = 5
            const val STARTED = 6
            const val ENDED = 7
            const val CANCELED = 8
            const val STOPPED = 9
            const val ENABLED = 10
            const val DISABLED = 11
            const val CONNECTED = 12
            const val DISCONNECTED = 13
            const val UPDATED = 14
            const val DONE = 15
            const val DELETED = 16
            const val RESPONSE = 17
        }


        val tms = TmsGb(Tstate::class).onInit { cls -> Tstate() }
    }

    // ---------------------------- délégués Kotlin
    @TMignore
    var created_ by created
    @TMignore
    var uploaded_ by uploaded
    @TMignore
    var permission_ by permission
    @TMignore
    var granted_ by granted
    @TMignore
    var denied_ by denied
    @TMignore
    var loaded_ by loaded
    @TMignore
    var started_ by started
    @TMignore
    var ended_ by ended
    @TMignore
    var canceled_ by canceled
    @TMignore
    var stopped_ by stopped
    @TMignore
    var enabled_ by enabled
    @TMignore
    var disabled_ by disabled
    @TMignore
    var connected_ by connected
    @TMignore
    var disconnected_ by disconnected
    @TMignore
    var updated_ by updated
    @TMignore
    var done_ by done
    @TMignore
    var deleted_ by deleted
    @TMignore
    var response_ by response

    @TMignore
    var sent_ by sent
    @TMignore
    var received_ by received
    @TMignore
    var read_ by read

    @TMignore
    var status_ by status


    override fun clazz(): Tclass {
        if (_clazz == null) _clazz = Tclass(Tstate::class, super.clazz()).fields(
            created.apply { name = "created" },
            uploaded.apply { name = "uploaded" },
            permission.apply { name = "permission" },
            granted.apply { name = "granted" },
            denied.apply { name = "denied" },
            loaded.apply { name = "loaded" },
            started.apply { name = "started" },
            ended.apply { name = "ended" },
            canceled.apply { name = "canceled" },
            stopped.apply { name = "stopped" },
            enabled.apply { name = "enabled" },
            disabled.apply { name = "disabled" },
            connected.apply { name = "connected" },
            disconnected.apply { name = "disconnected" },
            updated.apply { name = "updated" },
            done.apply { name = "done" },
            deleted.apply { name = "deleted" },
            response.apply { name = "response" },
            sent.apply { name = "sent" },
            received.apply { name = "received" },
            read.apply { name = "read" },
            status.apply { name = "status" }
        )
        return _clazz!!
    }

    @Suppress("UNCHECKED_CAST")
    override fun clazz(inst: TobGb): Tclass {
        inst as Tstate

        if (_clazz == null) _clazz = Tclass(Tstate::class, super.clazz()).fields(
            inst.created.apply { name = "created" },
            inst.uploaded.apply { name = "uploaded" },
            inst.permission.apply { name = "permission" },
            inst.granted.apply { name = "granted" },
            inst.denied.apply { name = "denied" },
            inst.loaded.apply { name = "loaded" },
            inst.started.apply { name = "started" },
            inst.ended.apply { name = "ended" },
            inst.canceled.apply { name = "canceled" },
            inst.stopped.apply { name = "stopped" },
            inst.enabled.apply { name = "enabled" },
            inst.disabled.apply { name = "disabled" },
            inst.connected.apply { name = "connected" },
            inst.disconnected.apply { name = "disconnected" },
            inst.updated.apply { name = "updated" },
            inst.done.apply { name = "done" },
            inst.deleted.apply { name = "deleted" },
            inst.response.apply { name = "response" },
            inst.sent.apply { name = "sent" },
            inst.received.apply { name = "received" },
            inst.read.apply { name = "read" },
            inst.status.apply { name = "status" }
        )
        return _clazz!!
    }

}