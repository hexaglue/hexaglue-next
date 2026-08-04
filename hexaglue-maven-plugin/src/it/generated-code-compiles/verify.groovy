def log = new File(basedir, 'build.log').text
def generated = new File(basedir, 'target/generated-sources/hexaglue/com/example/shop')
def classes = new File(basedir, 'target/classes/com/example/shop')

assert log.contains('HexaGlue wrote') : 'the goal should say what it wrote: ' + log

// What the backend writes for this domain, named one by one: a miscount would pass a laxer check.
['OrderEntity', 'MoneyEmbeddable', 'OrderMapper', 'MoneyMapper', 'OrderJpaRepository', 'OrdersJpaAdapter'].each {
    assert new File(generated, it + '.java').exists() : it + '.java should have been generated, found ' + generated.list()
}

// The point of the whole exercise: what was written is handed to the compiler and it accepts it.
['OrderEntity', 'MoneyEmbeddable', 'OrderMapper', 'MoneyMapper', 'OrderJpaRepository', 'OrdersJpaAdapter'].each {
    assert new File(classes, it + '.class').exists() : it + '.class should have been compiled, found ' + classes.list()
}

// Written where a build writes, never among the author's own sources.
assert !new File(basedir, 'src/main/java/com/example/shop/OrderEntity.java').exists() :
        'nothing should be written into the project sources'

// Read back on the next run rather than mistaken for the architecture.
assert new File(generated, 'OrdersJpaAdapter.java').text.contains('@Generated("io.hexaglue.jpa")') :
        'a generated type should say it was generated'

// What is written is held to the letter. A generator whose output drifts silently is a generator
// whose next release rewrites everybody's tree; the expected files are read as the reference and
// refreshed on purpose, by copying the generated ones over them.
def expected = new File(basedir, 'expected')
expected.eachFile { reference ->
    def actual = new File(generated, reference.name)
    assert actual.exists() : reference.name + ' was expected but not generated'
    assert actual.text == reference.text :
            reference.name + ' was generated differently than expected:\n--- expected ---\n' +
            reference.text + '\n--- generated ---\n' + actual.text
}

return true
