package ai.tambo.tmodeler.ms.ext

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.db.Ttable
import ai.tambo.tmodeler.db.TtableField
import ai.tambo.tmodeler.field.advanced.GeoField
import ai.tambo.tmodeler.field.advanced.JsonField
import ai.tambo.tmodeler.field.advanced.ListFieldGb
import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.base.TF
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.base.tf_str
import ai.tambo.tmodeler.field.media.FileField
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.utils.Ttuple

import kotlinx.datetime.*

fun <T : TBaseModel> TmsGb<T>.basePrepare() {
    if (_isReady) return

    if (_instance == null) {
        _instance = _prototype
        _tuplet = Ttuple(listOf(_prototype))
    }

    if (_modelClass == null) {
        _modelClass = _prototype?.clazz()?:_instance?.clazz()
    }
}
fun <T : TBaseModel> TmsGb<T>.prepare() {
    try {
        if (_isReady) return

        val v = _prototype?.clazz()?.modelName()
        basePrepare()

        if(_modelClass!=null){
            _isReady = setup(_modelClass!!)

            initCallbacks()

            if(_isReady) {
                // config syncMaster
                if(_prototype?.onSync()?.isConfig()?:false){
                    TmsDeep.sync(this)
                }
                // config securityMaster
                //_prototype?.onSync()
            }
        }
    }catch (_: Exception){}
}
fun <T : TBaseModel> TmsGb<T>.setup(modelClass: Tclass?) : Boolean {
    if (modelClass == null) return false

    val modelPackage = modelClass.getPackage()
    val className = modelClass.name()
    val modelName = modelClass.modelName()
    val attrs = modelClass.getAttrs()
    val fields = mutableListOf<BaseField>()

    val modelFields = mutableListOf<TtableField>()
    val primaryKeys = mutableListOf<String>()

    for (attr in attrs) {
        val baseField = attr as? BaseField ?: continue

        fields.add(baseField)

        if (baseField.getConstraints().isPrimaryKey()) {
            primaryKeys.add(baseField.name)
        }
    }

    for (field in fields) {
        if(!contentMedia && field is FileField) contentMedia = true
        if(!contentModel && !field.isCoreField() && field is ModelFieldGb<*>) contentModel = true
        if(!contentList && !field.isCoreField() && field is ListFieldGb<*>) contentList = true
        if(!contentDict && !field.isCoreField() && (field is ListFieldGb<*> || field is JsonField)) contentDict = true
        if(!contentLink && (contentModel || contentList)) contentLink = true

        val fieldName = field.name
        val fieldType = field.getType()
        val fieldConstraints = field.getConstraints()
        val fieldOnDelete = field.getOnDelete()
        val fieldDefaultValue = field.getValue()
        val fieldMaxLen = field.getMaxLen()

        val adjustedType = if (fieldType == TFtype.TEXT && fieldMaxLen > 0) {
            TFtype.VARCHAR
        } else {
            fieldType
        }

        val tField = TtableField(fieldName, tf_str(adjustedType))

        if (fieldConstraints.isPrimaryKey() && primaryKeys.size > 1) {
            fieldConstraints.removeIfExist(TF.PRIMARY_KEY)
        }

        if (fieldConstraints.isForeignKey()) {
            try {
                if(field is ModelFieldGb<*>) field.prepare()
            }catch (_: Exception){}

            val fieldTargetField = field.getTargetField()
            val fieldTargetModel = field.getTargetModel()

            fieldConstraints.removeIfExist(TF.FOREIGN_KEY)
            tField.targetField(fieldTargetField)
            tField.onDelete(tf_str(fieldOnDelete))
            tField.targetTable(fieldTargetModel.modelName())
        }

        if (field is GeoField) {
            tField.geoType(GeoField.typeToStr(field._geoType))
                .geoSrid(field._srid)
                .geoDim(field._dim)
                .geoMeasure(field._measure)
                .geoSpatialIndex(field._spatialIndex)
        }

        tField.defaultValue(fieldDefaultValue)
            .constraints(fieldConstraints.str())
            .maxlen(fieldMaxLen)

        modelFields.add(tField)
    }

    val table = Ttable(modelName, modelFields, primaryKeys)
    val db = TModeler.getInstance().geTdb(modelClass)
    db.createTable(table)

    val res = db.tableExists(modelClass.modelName())

    if(res) register()

    return res
}
/*fun <T : TBaseModel> TmsGb<T>.setup(modelClass: Tclass?) : Boolean {
    if (modelClass == null) return false

    val modelPackage = modelClass.getPackage()
    val className = modelClass.name()
    val modelName = modelClass.modelName()
    val attrs = modelClass.getAttrs()
    val fields = mutableListOf<BaseField>()

    val modelFields = mutableListOf<TtableField>()
    val primaryKeys = mutableListOf<String>()

    for (attr in attrs) {
        val baseField = attr as? BaseField ?: continue

        fields.add(baseField)

        if (baseField.getConstraints().isPrimaryKey()) {
            primaryKeys.add(baseField.name)
        }
    }

    for (field in fields) {
        val fieldName = field.name
        val fieldType = field.getType()
        val fieldConstraints = field.getConstraints()
        val fieldOnDelete = field.getOnDelete()
        val fieldDefaultValue = field.getValue()
        val fieldMaxLen = field.getMaxLen()

        val adjustedType = if (fieldType == TFtype.TEXT && fieldMaxLen > 0) {
            TFtype.VARCHAR
        } else {
            fieldType
        }

        val tField = TtableField(fieldName, tf_str(adjustedType))

        if (fieldConstraints.isPrimaryKey() && primaryKeys.size > 1) {
            fieldConstraints.removeIfExist(TF.PRIMARY_KEY)
        }

        if (fieldConstraints.isForeignKey()) {
            try {
                if(field is ModelFieldGb<*>) field.prepare()
            }catch (_: Exception){}

            val fieldTargetField = field.getTargetField()
            val fieldTargetModel = field.getTargetModel()

            fieldConstraints.removeIfExist(TF.FOREIGN_KEY)
            tField.targetField(fieldTargetField)
            tField.onDelete(tf_str(fieldOnDelete))
            tField.targetTable(fieldTargetModel.modelName())
        }

        if (field is GeoField) {
            tField.geoType(GeoField.typeToStr(field._geoType))
                .geoSrid(field._srid)
                .geoDim(field._dim)
                .geoMeasure(field._measure)
                .geoSpatialIndex(field._spatialIndex)
        }

        tField.defaultValue(fieldDefaultValue)
            .constraints(fieldConstraints.str())
            .maxlen(fieldMaxLen)

        modelFields.add(tField)
    }

    val table = Ttable(modelName, modelFields, primaryKeys)
    val db = TModeler.getInstance().geTdb(modelClass)
    db.createTable(table)

    val res = db.tableExists(modelClass.modelName())

    if(res) register()

    return res
}*/
fun <T : TBaseModel> TmsGb<T>.tableFields(modelClass: Tclass?) : List<TtableField> {
    if (modelClass == null) return emptyList()

    val attrs = modelClass.getAttrs()
    val fields = mutableListOf<BaseField>()
    val modelFields = mutableListOf<TtableField>()

    for (attr in attrs) {
        val baseField = attr as? BaseField ?: continue

        fields.add(baseField)
    }

    for (field in fields) {
        val fieldName = field.name
        val fieldType = field.getType()
        val fieldConstraints = field.getConstraints()
        val fieldOnDelete = field.getOnDelete()
        val fieldDefaultValue = field.getValue()
        val fieldMaxLen = field.getMaxLen()
        val fieldTargetField = field.getTargetField()

        val adjustedType = if (fieldType == TFtype.TEXT && fieldMaxLen > 0) {
            TFtype.VARCHAR
        } else {
            fieldType
        }

        val tField = TtableField(fieldName, tf_str(adjustedType))

        modelFields.add(tField)
    }

    return modelFields
}
fun <T : TBaseModel> TmsGb<T>.register() = TmsDeep.registerTms(listOf(this))
fun <T : TBaseModel> TmsGb<T>.find(tms: String) = TmsDeep.findTms(tms)
