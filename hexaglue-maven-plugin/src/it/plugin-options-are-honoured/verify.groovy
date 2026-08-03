def output = new File(basedir, 'target/custom-reports')

// Where the build says, not where the goal would have chosen.
assert output.exists() : 'the goal should write where the build states: ' + new File(basedir, 'target').list()
assert !new File(basedir, 'target/hexaglue').exists() : 'nothing should be written at the default place'

// Each backend writes under the directory it was asked for, inside that one.
def audit = new File(output, 'audit/architecture-audit.md')
assert audit.exists() : 'the audit report should be under the directory it was asked for: ' + output.list()
assert !new File(output, 'audit/architecture-audit.json').exists() : 'the data form was refused and should be absent'

def domain = new File(output, 'docs/domain.md')
assert domain.exists() : 'the living documentation should be under the directory it was asked for: ' + output.list()

// The fixture is written to classify by position alone, with no framework symbol anywhere:
// PlaceOrder holds Orders, nobody implements it, so it is a driven port, and what it keeps is
// an aggregate. Without this the pages below would be empty and the assertions vacuous.
assert domain.text.contains('### Order') : 'the domain page should document the aggregate: ' + domain.text

// A budget of one property is honoured, so the diagram shows the identity and stops there.
def diagram = domain.text.substring(domain.text.indexOf('```mermaid'), domain.text.indexOf('## Aggregates'))
assert diagram.contains('+OrderId id') : 'the diagram should show the first property: ' + diagram
assert !diagram.contains('+Money total') : 'the diagram should stop at the budget it was given: ' + diagram

// Provenance was refused, so no verdict unfolds what it rests on.
assert !domain.text.contains('What this verdict rests on') : 'provenance was refused and should be absent'

return true
