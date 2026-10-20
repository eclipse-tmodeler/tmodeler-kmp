package ai.tambo.tmodeler.field.media

import ai.tambo.tmodeler.field.media.utils.DocType
import ai.tambo.tmodeler.field.media.utils.TFile

class DocField : FileField() {

    // Liste interne qui contient les formats autorisés (ext + mime)
    private val types = mutableListOf<DocType>()

    init {

        include(*DocType.entries.toTypedArray())
    }

    /**
     * Inclure uniquement certains types de documents
     * Remplace entièrement les listes allowedExtensions & allowedMimeTypes
     */
    fun include(vararg includeTypes: DocType): DocField {
        types.clear()
        types.addAll(includeTypes)

        allowedExtensions = types.map { it.ext }
        allowedMimeTypes = types.map { it.mime }

        return this
    }

    /**
     * Exclure certains types de documents
     */
    fun exclude(vararg excludeTypes: DocType): DocField {
        types.removeAll(excludeTypes.toSet())

        allowedExtensions = types.map { it.ext }
        allowedMimeTypes = types.map { it.mime }

        return this
    }

    override fun isValid(v: Any): Boolean {
        if (!super.isValid(v)) return false

        if (v is TFile) {
            // Ici tu peux ajouter des validations spécifiques pour documents
            // Exemple : taille max, nombre de pages, etc.
        }

        return true
    }
}
