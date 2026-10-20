package ai.tambo.tmodeler.samples

import ai.tambo.tmodeler.field.advanced.BoolField
import ai.tambo.tmodeler.field.advanced.EnumField
import ai.tambo.tmodeler.field.advanced.JsonField
import ai.tambo.tmodeler.field.advanced.ListField
import ai.tambo.tmodeler.field.advanced.ModelField
import ai.tambo.tmodeler.field.advanced.TimeField
import ai.tambo.tmodeler.field.common.FloatField
import ai.tambo.tmodeler.field.common.IntField
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.model.TMignore
import ai.tambo.tmodeler.model.TModel
import ai.tambo.tmodeler.ms.Tms
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonElement

/*
 * Modèles d'exemple utilisés par toute la suite de tests.
 *
 * Anatomie d'un modèle TModeler :
 *   1. une classe qui hérite de TModel<MaClasse>
 *   2. un `var` par colonne, dont la valeur est un champ (TextField, IntField, ...)
 *   3. un accès à la valeur `xxx_ by xxx`, annoté @TMignore (ce n'est PAS une colonne)
 *   4. un gestionnaire `companion object { val tms ... }` : c'est la porte d'entrée (Author.tms.all(), ...)
 *
 * Convention : `author.name` est le CHAMP (options, requêtes), `author.name_` est la VALEUR.
 */

enum class Status { DRAFT, PUBLISHED, ARCHIVED }

class Author : TModel<Author>() {
    var name = TextField()
    var email = TextField().unique()
    var age = IntField().default(0)
    var rating = FloatField()
    var active = BoolField().default(true)

    @TMignore var name_ by name
    @TMignore var email_ by email
    @TMignore var age_ by age
    @TMignore var rating_ by rating
    @TMignore var active_ by active

    companion object {
        val tms by lazy { Tms(Author::class) }
    }
}

class Tag : TModel<Tag>() {
    var label = TextField()

    @TMignore var label_ by label

    companion object {
        val tms by lazy { Tms(Tag::class) }
    }
}

class Book : TModel<Book>() {
    var title = TextField()
    var pages = IntField().default(0)
    var status = EnumField(Status.values()).default(Status.DRAFT)

    // relation « un-à-un » : un livre a UN auteur
    var author = ModelField(Author::class)

    // relation « un-à-plusieurs » : un livre a PLUSIEURS tags
    var tags = ListField(Tag::class)

    @TMignore var title_ by title
    @TMignore var pages_ by pages
    @TMignore var status_ by status
    @TMignore var author_ by author
    @TMignore var tags_ by tags

    companion object {
        val tms by lazy { Tms(Book::class) }
    }
}

/** Modèle dédié aux champs « temps » et « json ». */
class Note : TModel<Note>() {
    var text = TextField()
    var at = TimeField()
    var payload = JsonField()

    @TMignore var text_ by text
    @TMignore var at_ by at
    @TMignore var payload_ by payload

    companion object {
        val tms by lazy { Tms(Note::class) }
    }
}
