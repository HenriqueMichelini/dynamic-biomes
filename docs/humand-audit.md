The ore inspect command now delegates policy and origin lookup to
`OreInspectionService`. `OreInspectDiagnostic` translates the target block and
formats the result. The root command retains routing and target selection.
