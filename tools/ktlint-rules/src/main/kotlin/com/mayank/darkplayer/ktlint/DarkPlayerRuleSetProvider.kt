package com.mayank.darkplayer.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class DarkPlayerRuleSetProvider : RuleSetProviderV3(id = RuleSetId(value = "darkplayer")) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(RuleProvider { CallLayoutRule() })
}
