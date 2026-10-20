package ai.tambo.tmodeler.support

import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.samples.Status
import ai.tambo.tmodeler.samples.Tag

/** Construit un auteur en mémoire (non enregistré). */
fun author(name: String, age: Long = 30L, rating: Double = 3.0, active: Boolean = true) = Author().also {
    it.name_ = name
    it.email_ = "${name.lowercase()}@example.com"
    it.age_ = age
    it.rating_ = rating
    it.active_ = active
}

fun book(title: String, pages: Long = 100L, status: Status = Status.DRAFT, author: Author? = null) = Book().also {
    it.title_ = title
    it.pages_ = pages
    it.status_ = status
    if (author != null) it.author_ = author
}

fun tag(label: String) = Tag().also { it.label_ = label }

/**
 * Jeu de données commun aux tests de requêtes (6 auteurs, enregistrés dans cet ordre) :
 *
 * | nom   | âge | note | actif |
 * |-------|-----|------|-------|
 * | Alice | 30  | 4.5  | oui   |
 * | Bob   | 25  | 3.0  | oui   |
 * | Carol | 35  | 5.0  | oui   |
 * | Dave  | 40  | 2.0  | non   |
 * | Eve   | 22  | 4.0  | oui   |
 * | Frank | 30  | 3.5  | oui   |
 */
fun seedAuthors(): List<Author> = inData {
    listOf(
        author("Alice", 30, 4.5),
        author("Bob", 25, 3.0),
        author("Carol", 35, 5.0),
        author("Dave", 40, 2.0, active = false),
        author("Eve", 22, 4.0),
        author("Frank", 30, 3.5),
    ).onEach { it.save() }
}
