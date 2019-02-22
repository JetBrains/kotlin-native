package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.ReifiedTypeParameterSubstitutionChecker
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.inline.ReasonableInlineRule
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesSettings

object KonanPlatformConfigurator : PlatformConfiguratorBase(
        DynamicTypesSettings(),
        additionalDeclarationCheckers = listOf(ExpectedActualDeclarationChecker()),
        additionalCallCheckers = listOf(
                org.jetbrains.kotlin.resolve.jvm.checkers.SuperCallWithDefaultArgumentsChecker(),
                ReifiedTypeParameterSubstitutionChecker()
        ),
        additionalTypeCheckers = listOf(),
        additionalClassifierUsageCheckers = listOf(),
        additionalAnnotationCheckers = listOf(),
        identifierChecker = IdentifierChecker.Default,
        overloadFilter = OverloadFilter.Default,
        platformToKotlinClassMap = PlatformToKotlinClassMap.EMPTY,
        delegationFilter = DelegationFilter.Default,
        overridesBackwardCompatibilityHelper = OverridesBackwardCompatibilityHelper.Default,
        declarationReturnTypeSanitizer = DeclarationReturnTypeSanitizer.Default
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(SyntheticScopes.Empty)
        container.useInstance(TypeSpecificityComparator.NONE)
        container.useInstance(SamConversionTransformer.Empty)
        container.useInstance(NativeInliningRule)
    }
}

object NativeInliningRule : ReasonableInlineRule {
    override fun isInlineReasonable(
            descriptor: CallableMemberDescriptor,
            declaration: KtCallableDeclaration,
            context: BindingContext
    ): Boolean = true
}