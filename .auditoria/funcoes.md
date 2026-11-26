# Funções do Wake Wake Up: o que funciona, o que quebrou

Gerado em 2026-11-26 pelo AGENTE TESTADOR. Suíte: 270 testes (189 antes), 0 falhas, 26 ignorados (24 antigos do ScreenshotTour, 2 novos de bugs).

Status: FUNCIONA (um teste automatizado prova), QUEBRADO (há teste de bug ignorado em `*BugTest`), NÃO TESTÁVEL (não dá para provar na JVM).

| id | função | status | teste que prova | motivo/observação |
|---|---|---|---|---|
| F01 | Criar alarme | FUNCIONA | AlarmListUiTest.creatingAnAlarmFromScratch; AlarmRepositoryTest.savingANewAlarmSchedulesItsNextOccurrenceAsAnAlarmClock; AlarmListUiTest.leavingANewAlarmSavesNothing.; AlarmRepositoryEdgeTest.twoAlarmsAtTheSameTimeBothStayArmed |  |
| F02 | Editar alarme | FUNCIONA | AlarmListUiTest.editingAnAlarm; AlarmRepositoryTest.editingAnAlarmReplacesItsSchedule.; AlarmRepositoryEdgeTest.editingTheTimeMovesTheRingInsteadOfAddingAnotherOne |  |
| F03 | Sair do editor sem salvar (descartar) | FUNCIONA | AlarmListUiTest.leavingTheEditorWithBackDiscardsTheChanges; AlarmListUiTest.theSystemBackAsksBeforeLosingAChange; AlarmListUiTest.theSystemBackLeavesAnUntouchedAlarmAtOnce. |  |
| F04 | Excluir alarme (dois toques) | FUNCIONA | AlarmListUiTest.deletingTakesTwoTaps; AlarmRepositoryTest.deletingCancels.; AlarmRepositoryEdgeTest.deletingOneAlarmKeepsTheOthersArmed |  |
| F05 | Ligar e desligar alarme | FUNCIONA | AlarmListUiTest.switchingAnAlarmBackOn; AlarmRepositoryTest.turningOffCancelsAndTurningOnReschedules.; AlarmRepositoryEdgeTest.savingASwitchedOffAlarmLeavesNothingInAlarmManager |  |
| F06 | Escolher dias da semana | FUNCIONA | AlarmEntityTest.daysAreStoredSortedAsText; AlarmListUiTest.aCardReadsItsDaysAsOnePhrase; AlarmTimingTest.weekdayAlarmOnFridayNightRingsMonday; AlarmTimingTest.weekendAlarmFromMonday; AlarmTimingTest.dayIndexesRunMondayToSunday.; AlarmTimingEdgeTest.aSingleWeekdayPassedTodayWaitsExactlyAWeek; AlarmTimingEdgeTest.theNextRingIsAlwaysAheadOnAMatchingDayAtTheRightTime |  |
| F07 | Definir hora e minuto (roda, passos de 10 min) | FUNCIONA | AlarmListUiTest.theTimeDrumsWorkWithTalkBack; AlarmTimingTest.midnightAlarm.; AlarmTimingEdgeTest.theSameMinuteAsTheAlarmIsAlreadyTomorrow; AlarmTimingEdgeTest.oneSecondBeforeTheAlarmStillRingsToday |  |
| F08 | Nome do alarme (rótulo) | FUNCIONA | AlarmLabelUiTest.theNameIsReplacedAndShownOnTheList; AlarmLabelUiTest.anEmptyNameShowsTheUntitledPlaceholderOnTheList; AlarmLabelUiTest.accentsAndEmojiInTheNameSurviveTheDatabase |  |
| F09 | Ordem da lista por horário | FUNCIONA | AlarmListUiTest.alarmsStayInTimeOrderWhetherOnOrOff. |  |
| F10 | Próximo alarme no visor (contagem) | FUNCIONA | AlarmListUiTest.theNextDisplayCountsDownToTheNearestAlarm; AlarmListUiTest.aRingADayOrMoreAwayShowsItsDayAndTime; AlarmListUiTest.aRingAWeekOrMoreAwaySaysItIsTheNextOne.; AlarmTimingEdgeTest.theLastNightOfTheYearRingsOnNewYearsDay; AlarmTimingEdgeTest.theTwentyNinthOfFebruaryExistsOnlyInLeapYears |  |
| F11 | Estado vazio (convite para criar o primeiro) | FUNCIONA | AlarmListUiTest.anEmptyListInvitesToCreateTheFirstAlarm. |  |
| F12 | Excluir e tocar em "+" (novo alarme) | FUNCIONA | ver F01 | Coberta por F01. |
| F13 | Pular só o próximo dia (lista, gaveta) | FUNCIONA | AlarmRepositoryTest.skippingTheNextDayKeepsTheAlarmOnAndArmsTheRingAfterIt; AlarmTimingTest.skippingADayRingsAtTheFollowingOccurrence; AlarmTimingTest.aSkipOnlyCountsWhenItsDayIsTheNextOccurrence; AlarmTimingTest.aSkipAfterMidnightStillNamesTheMorningAhead; AlarmListUiTest.switchingARecurringAlarmOffOffersToSkipJustOneDay.; AlarmTimingEdgeTest.skippingTheOnlyWeekdayJumpsTwoWeeks; AlarmTimingEdgeTest.skippingTodayWhenTodayStillComesRingsTomorrow; AlarmRepositoryEdgeTest.theRingAfterASkippedOneIsStillArmedWhenTheSkipIsSet |  |
| F14 | Pular um alarme de uma vez só (sem dias) = desligar | FUNCIONA | AlarmRepositoryTest.skippingAOneTimeAlarmSwitchesItOff; AlarmListUiTest.switchingAOneOffAlarmOffDoesNotOfferTheSkip; AlarmTimingTest.oneTimeAndSwitchedOffAlarmsIgnoreASkip.; AlarmRepositoryEdgeTest.skippingThenUndoingAOneOffRingBringsItBack |  |
| F15 | Desfazer o pulo do dia (voltar a tocar) | FUNCIONA | AlarmRepositoryTest.undoingTheSkipRingsOnTheSkippedDayAgain. |  |
| F16 | Ligar de volta é o mesmo que tirar o pulo; ligar/desligar à mão apaga o pulo | FUNCIONA | AlarmRepositoryTest.switchingByHandDropsASkip. |  |
| F17 | Pulo some ao salvar se o dia já passou ou o alarme está desligado | FUNCIONA | AlarmRepositoryTest.savingKeepsASkipOnlyWhileItStillFits. |  |
| F18 | Pular um toque específico pela notificação "Já acordou?" (confirmar com conta) | FUNCIONA | SkipConfirmUiTest.turningOffTakesASumAndCanBeUndone; SkipConfirmUiTest.keepLeavesTheAlarmAlone; SkipConfirmUiTest.aRingThatAlreadyChangedCannotBeTurnedOff; AlarmRepositoryTest.skippingOneRingOnlyWorksWhileThatRingIsNextAndAhead; AlarmRepositoryTest.skippingOneRingOfAOneTimeAlarmSwitchesItOffAndUndoBringsItBack.; AlarmRepositoryEdgeTest.aRingLessThanAMinuteAwayCannotBeSkipped; AlarmRepositoryEdgeTest.onlyTheNextRingCanBeSkippedNotALaterOne; AlarmRepositoryEdgeTest.aSwitchedOffAlarmCannotBeSkipped; AlarmRepositoryEdgeTest.skippingAnUnknownAlarmIsRefused |  |
| F19 | Desfazer o pulo de um toque (pelo recibo ou pela tela) | FUNCIONA | PreAlarmCheckTest.theReceiptsUndoPutsTheRingBack; SkipConfirmUiTest.turningOffTakesASumAndCanBeUndone.; AlarmRepositoryEdgeTest.undoingASkipTooCloseToTheRingIsRefused; AlarmRepositoryEdgeTest.undoingASkipOfAnotherDayChangesNothing |  |
| F20 | Skip de um dia que já passou é ignorado (limpeza no boot ou no reagendamento) | FUNCIONA | AlarmRepositoryTest.rescheduleAllForgetsSkipsWhoseDayIsOver; AlarmReceiverTest.aRingAfterTheSkippedDayClearsTheSkip.; AlarmRepositoryEdgeTest.armingTheNextRingForgetsASkipWhoseDayHasCome |  |
| F21 | Perguntar, uma hora antes, sobre alarmes matinais (03:00 a 11:59) | FUNCIONA | PreAlarmPlannerTest.nothingIsAskedUntilAnHourBefore; PreAlarmPlannerTest.withinTheHourTheMorningAlarmIsOffered; PreAlarmPlannerTest.onlyMorningAlarmsAreEverQuestioned; PreAlarmCheckTest.anEveningAlarmIsNeverQuestioned; PreAlarmCheckTest.theFirstCheckIsArmedAnHourBeforeAndNeverWakesThePhone. |  |
| F22 | Agrupar alarmes de reserva numa só pergunta | FUNCIONA | PreAlarmPlannerTest.backupAlarmsComeTogetherInOneOffer; PreAlarmCheckTest.backupAlarmsShareOneOffer. |  |
| F23 | Não perguntar nos últimos 5 min antes de um toque | FUNCIONA | PreAlarmPlannerTest.neverInTheLastFiveMinutesBeforeARing; PreAlarmPlannerTest.anyAlarmAboutToRingHoldsTheOfferBack. |  |
| F24 | Não perguntar se o app foi aberto há menos de 90 min | FUNCIONA | PreAlarmPlannerTest.havingTheAppOpenSinceBeforeMeansNoNeedToAsk. |  |
| F25 | Cada toque é perguntado uma só vez | FUNCIONA | PreAlarmPlannerTest.eachRingIsAskedAboutOnce; PreAlarmCheckTest.theSameRingIsNeverAskedAboutTwice. |  |
| F26 | Desligados e pulados ficam de fora da pergunta | FUNCIONA | PreAlarmPlannerTest.switchedOffAndSkippedRingsAreLeftAlone. |  |
| F27 | Não perguntar com o telefone em uso ou com a tela desligada (espera) | FUNCIONA | PreAlarmCheckTest.phoneInUse (helper); PreAlarmCheckTest.aPhoneInUseGetsOneSilentOffer; PreAlarmCheckTest.withTheScreenOffItWaitsWithoutWakingThePhone; PreAlarmCheckTest.aLockedScreenIsNotInUse. |  |
| F28 | Nada é perguntado durante um toque | FUNCIONA | PreAlarmCheckTest.nothingIsAskedWhileAnAlarmIsRinging; PreAlarmCheckTest.aRingingAlarmWithdrawsTheOfferToTurnItOff (em AlarmRingServiceTest). |  |
| F29 | Retirar a oferta se o alarme mudou | FUNCIONA | PreAlarmCheckTest.anOfferWhoseAlarmChangedIsWithdrawn. |  |
| F30 | Sem permissão de notificação, não há checagem | FUNCIONA | PreAlarmCheckTest.withoutTheNotificationPermissionThereAreNoChecks. |  |
| F31 | Mostrar recibo de pulo com opção de desfazer | FUNCIONA | PreAlarmCheckTest.theReceiptsUndoPutsTheRingBack. |  |
| F32 | Tocar no horário (som, tela cheia sobre a tela de bloqueio) | FUNCIONA | AlarmReceiverTest.anEnabledAlarmStartsTheRingingServiceAndSchedulesItsNextOccurrence; AlarmReceiverTest.aOneOffAlarmRingsAndIsNotScheduledAgain; AlarmReceiverTest.aDisabledAlarmNeverRings; AlarmReceiverTest.aDeletedAlarmOrABrokenIntentIsIgnored; AlarmRingServiceTest.ringsTheChosenSoundOnLoopStartingLow.; AlarmRingServiceEdgeTest.anAlarmDeletedBeforeTheServiceStartsRingsNothing | Disparo, serviço e som provados na JVM. NÃO TESTÁVEL de verdade: a tela por cima da tela bloqueada (showWhenLocked/turnScreenOn), o setAlarmClock real e o toque em Doze exigem aparelho. |
| F33 | Alarme de uma vez se desliga ao tocar; repetido continua ligado | FUNCIONA | AlarmRingServiceTest.aOneOffAlarmTurnsItselfOffAsItRings; AlarmRingServiceTest.aRepeatingAlarmStaysOnWhenItRings; AlarmRingServiceTest.aRepeatingAlarmKeepsItsScheduleThroughTheWholeRitual. |  |
| F34 | Som do alarme: sons do app (5 sons incluídos) | FUNCIONA | SoundAndTaskUiTest.choosingOneOfTheAppsSoundsAndTurningTheRampOff; AlarmRingServiceTest.withNoSystemAlarmSoundTheBundledBellRings. |  |
| F35 | Som do celular: toques do celular e padrão do sistema | FUNCIONA | SoundAndTaskUiTest.goingBackToTheSystemDefault; AlarmRingServiceTest.aSoundThatCantPlayFallsBackToTheSystemAlarm; AlarmRingServiceTest.aBrokenSystemAlarmSoundFallsBackToTheBundledBell. |  |
| F36 | Música do celular (escolher arquivo) | FUNCIONA | SoundAndTaskUiTest.pickingASongFromThePhone. |  |
| F37 | Rádio da internet: escolher uma estação popular ou buscar por nome | FUNCIONA | SoundAndTaskUiTest.wakingUpToARadioStation; SoundAndTaskUiTest.radioWithoutInternetCanRetryAndSearch; RadioBrowserTest.keepsOnlyNamedHttpsStationsOncePerStream; RadioBrowserTest.aStationWithoutACountryCodeKeepsAnEmptyOne; RadioBrowserTest.aBlankQueryAsksForTheMostListenedInTheCountry; RadioBrowserTest.aQuerySearchesByName; RadioBrowserTest.noNetworkIsAnError; RadioBrowserTest.anUnexpectedAnswerIsAnErrorToo. |  |
| F38 | Rádio que não começa é trocado pelo alarme padrão após 8 s | FUNCIONA | AlarmRingServiceTest.aRadioThatDoesntStartIsReplacedAfterEightSeconds. |  |
| F39 | Rádio que tocando sem reação dá lugar ao alarme em volume total (carência de 3 min) | FUNCIONA | AlarmRingServiceTest.aRadioNobodyReactsToGivesWayToTheAlarmAtFullVolume. |  |
| F40 | Aumentar aos poucos (rampa de 15% a 100% em 60 s) | FUNCIONA | AlarmRingServiceTest.volumeClimbsToFullInAMinute; AlarmRingServiceTest.withoutTheRampItRingsAtFullVolumeAtOnce; RingFlowUiTest.theVolumeMeterRisesWithTheRamp; SoundAndTaskUiTest.choosingOneOfTheAppsSoundsAndTurningTheRampOff. |  |
| F41 | Vibração enquanto toca, parando com o som | FUNCIONA | AlarmRingServiceTest.itVibratesWhileItRingsAndStopsWithTheSound. |  |
| F42 | Notificação do toque diz o que está acontecendo | FUNCIONA | AlarmRingServiceTest.theNotificationSaysWhatTheAlarmIsDoing. |  |
| F43 | Voltar não dispensa o alarme (tela do toque) | FUNCIONA | RingFlowUiTest.backDoesntDismissTheAlarm. |  |
| F44 | Tela do toque ao abrir depois que o alarme acabou volta ao app | FUNCIONA | RingFlowUiTest.aRingScreenOpenedAfterTheAlarmEndedGoesBackToTheApp. |  |
| F45 | Tela do toque (hora, botões Espere e Começar missão) | FUNCIONA | RingFlowUiTest.theWholeRitualFromRingingToGoodMorning; PortugueseUiTest.theRingScreen. |  |
| F46 | Espere: 5 min de silêncio, conta de quantas vezes, volta em volume baixo | QUEBRADO | AlarmRingServiceTest.espereSilencesForFiveMinutesAndCountsOnce; AlarmRingServiceTest.afterEspereItRingsAgainFromLow; AlarmRingServiceTest.espereKeepsThePhoneAwakeEvenAfterAQuarterOfAnHour; AlarmRingServiceTest.aSecondAlarmDuringEspereCanBeSnoozedToo; RingFlowUiTest.afterAnEspereTheBarCountsInsteadOfSayingNoLimit.; AlarmRingServiceEdgeTest.startingTheMissionDuringEspereEndsTheEspereForGood; AlarmRingServiceEdgeTest.theRecordedWakeUpCountsEveryEspere; SessionBugTest.anEspereTapArrivingDuringTheMissionIsIgnored (ignorado, BUG-1) | BUG-1: um toque em Espere que chega com a missão aberta conta uma espera a mais. O resto do Espere funciona. |
| F47 | Começar missão: silencia o som e mostra o primeiro problema | FUNCIONA | AlarmRingServiceTest.startingTheMissionStopsTheSoundAndShowsTheFirstProblem. |  |
| F48 | Missão matemática: teclado da calculadora (dígitos, C, apagar, sinal) | FUNCIONA | AlarmRingServiceTest.calculatorKeys; AlarmRingServiceTest.confirmingNothingDoesNothing; AlarmRingServiceTest.aWrongAnswerIsFlaggedAndTheNextKeyStartsOver; RingFlowUiTest.aStrayTapOnCantSolveKeepsTheSums.; AlarmRingServiceEdgeTest.theCalculatorTakesAtMostFourDigits; ...theSignKeyTogglesAMinus; ...aLoneMinusSignIsNeverAnAnswer; ...aKeyPressedBeforeTheMissionStartsChangesNothing |  |
| F49 | Problemas de matemática (fácil: soma de 1 dígito; média: soma de 2 dígitos; difícil: multiplicação) | FUNCIONA | MissionGeneratorTest.easyIsASumOfTwoSingleDigits; MissionGeneratorTest.mediumIsASumOfTwoTwoDigitNumbers; MissionGeneratorTest.hardIsAMultiplication.; MissionGeneratorEdgeTest.everyQuestionHasTheRightAnswerAndAPositiveOne; MissionGeneratorEdgeTest.harderLevelsGiveBiggerAnswersOnAverage; MissionGeneratorEdgeTest.theBiggestAnswerStillFitsTheCalculatorDisplay |  |
| F50 | Quantidade de problemas (1 a N) e avanço | FUNCIONA | SoundAndTaskUiTest.taskSetupDifficultyAndHowManyProblems; AlarmRingServiceTest.solvingEveryProblemEndsInGoodMorning; RingFlowUiTest.theWholeRitualFromRingingToGoodMorning; AlarmRingServiceTest.calculatorKeys. |  |
| F51 | Missão de frase do dia (digitar a frase, sem acento e sem diferenças de caixa) | FUNCIONA | MissionGeneratorTest.phraseIsTheSameAllDayAndChangesEveryDay; MissionGeneratorTest.phraseFollowsTheLanguage; MissionGeneratorTest.phrasesHaveNoAccentsAndAreTypeable; MissionGeneratorTest.normalizationIgnoresCaseAccentsPunctuationAndExtraSpaces; MissionGeneratorTest.phraseMatching; MissionGeneratorTest.matchedPrefixLightsTheDisplayAsYouType; AlarmRingServiceTest.aPhraseAlarmGoesStraightToThePhrase; RingFlowUiTest.aPhraseAlarm.; MissionGeneratorEdgeTest (phraseMatches, matchedPrefixLength, ciclo de 12 dias); AlarmRingServiceEdgeTest.aPhraseTypedWithCapsAndNoAccentsIsAccepted; ...aPhraseLongerThanEightyCharactersIsCutNotRejectedOutright |  |
| F52 | Prévia da frase do dia na configuração da tarefa | FUNCIONA | SoundAndTaskUiTest.theTaskPreviewShowsTodaysPhrase. |  |
| F53 | "Não consigo resolver" troca para a frase | FUNCIONA | AlarmRingServiceTest.givingUpSwitchesToThePhraseAndKeepsTheClockRunning; RingFlowUiTest.cantSolveItSwitchesToTypingThePhrase. |  |
| F54 | Tempo da missão (2 min); acabou, toca de novo mais alto (pelo menos 80%) | FUNCIONA | AlarmRingServiceTest.runningOutOfTimeRingsAgainLouder; AlarmRingServiceTest.theTimerCountsDownOncePerSecond; AlarmRingServiceTest.theMissionTimerKeepsThePhoneAwakeToo.; AlarmRingServiceEdgeTest.aTimedOutMissionStartsOverFromTheFirstProblem |  |
| F55 | Concluir a missão: grava o despertar e encerra o toque | FUNCIONA | AlarmRingServiceTest.theWakeUpIsSavedAsSoonAsTheMissionIsDone; AlarmRingServiceTest.finishingRecordsTheWakeUpAndStopsEverything; AlarmRingServiceTest.finishingTheMissionEndsTheWatch.; AlarmRingServiceEdgeTest.theRecordedWakeUpCountsEveryEspere; WakeHistoryRepositoryTest (todos); AlarmRingServiceEdgeTest.aSecondFinishAfterConcluirIsHarmless |  |
| F56 | Tela Bom dia (tempo que levou, esperas, Concluir) | FUNCIONA | RingFlowUiTest.theWholeRitualFromRingingToGoodMorning; RingFlowUiTest.theConcluirCaptionIsPartOfItsKey; PortugueseUiTest.listAndEditor (indireto). |  |
| F57 | Bom dia fecha sozinha em 10 min | FUNCIONA | AlarmRingServiceTest.bomDiaClosesByItselfAfterTenMinutes. |  |
| F58 | Bom dia oferece pular os outros alarmes matinais da próxima hora | FUNCIONA | PreAlarmPlannerTest.bomDiaOffersTheOtherMorningAlarmsDueWithinTheHour. |  |
| F59 | Voltar fecha a tela Bom dia | FUNCIONA | GoodMorningBackUiTest.backOnGoodMorningClosesTheSessionAndReturnsToTheApp |  |
| F60 | Ver estatísticas da semana (histórico de 7 dias) | FUNCIONA | StatsUiTest.withNoWakeUpsYet; StatsUiTest.aWeekOfWakeUps; StatsUiTest.wakingUpAfterMidnightCountsTheRightDelay; AlarmListUiTest.theStatsButtonOpensTheStats; RingFlowUiTest.seeStatsOpensTheStatsScreen.; WakeHistoryRepositoryTest.theWeekCoversTodayAndTheSixDaysBeforeAndNothingOlder; ...aShorterWindowDropsTheOlderDays; ...twoWakeUpsInTheSameDayAreBothKept |  |
| F61 | Link de privacidade nas estatísticas | FUNCIONA | StatsUiTest.thePrivacyPolicyOpensInTheBrowser; StatsUiTest.withoutABrowserThePrivacyLinkDoesNothing; PortugueseUiTest.thePrivacyLinkOpensThePortuguesePage. |  |
| F62 | Onboarding de permissões no primeiro uso | FUNCIONA | PermissionsUiTest.firstLaunchWalksThroughThePermissionsOnce; PermissionsUiTest.withEverythingAllowedTheAppOpensStraightOnTheList; AppSetupTest.everyPermissionAnAlarmNeedsIsDeclared. |  |
| F63 | Notificações pedidas no próprio app (só na primeira vez) | FUNCIONA | PermissionsUiTest.notificationsAreAskedInTheAppOnlyTheFirstTime. |  |
| F64 | Outras permissões abrem a página de configurações do sistema | FUNCIONA | PermissionsUiTest.theOtherPermissionsOpenTheirSettingsPages. |  |
| F65 | Permissão concedida nas configurações acende ao voltar | FUNCIONA | PermissionsUiTest.aPermissionGrantedInSettingsLightsUpOnReturn. |  |
| F66 | Lê as permissões do sistema (exato, tela cheia, notificações) | FUNCIONA | PermissionsUiTest.alarmPermissionsAreReadFromTheSystem. |  |
| F67 | Bateria: só recomendada | FUNCIONA | PermissionsUiTest.batteryIsOnlyRecommended. |  |
| F68 | Aviso na lista de permissões faltando; toque leva às permissões e volta com um toque | FUNCIONA | ListWarningsUiTest.missingPermissionsShowUnderTheNextDisplay; ListWarningsUiTest.theWarningOpensThePermissionsAndOneTapComesBack. |  |
| F69 | Aviso de volume do alarme baixo, com ajuste | FUNCIONA | ListWarningsUiTest.aLowAlarmVolumeIsFlaggedAndTurnedUp; ListWarningsUiTest.theVolumeOnlyMattersWhileAnAlarmIsOn; ListWarningsUiTest.aLoudEnoughPhoneGetsNoVolumeWarning. |  |
| F70 | Alarmes reagendados após reinício do celular | FUNCIONA | AlarmReceiverTest.afterARebootEveryAlarmThatIsOnIsArmedAtItsRealNextRing; AlarmRepositoryTest.rescheduleAllArmsEveryAlarmThatIsOnAtItsRealNextRing.; AlarmRepositoryEdgeTest.rescheduleAllNeverArmsAnAlarmThatIsOff; AlarmRepositoryEdgeTest.rescheduleAllWithSparingLeavesAJustDueRingInFlightAlone |  |
| F71 | Alarmes reagendados ao trocar de fuso horário | FUNCIONA | AppSetupTest.aNewTimeZoneMovesTheAlarmsToTheNewLocalTime. |  |
| F72 | Alarmes reagendados após atualização do app ou force stop (ao abrir) | FUNCIONA | AppSetupTest.anAppUpdateReschedulesEverything; AppSetupTest.otherBroadcastsDontTouchTheAlarms; AlarmRepositoryTest.theStartupRescheduleLeavesARingThatMayStillBeOnItsWayAlone. |  |
| F73 | Toque interrompido (processo morto, reinício do celular) continua tocando | FUNCIONA | AlarmRingServiceTest.aRingingSessionIsWrittenDownAndWatched; AlarmRingServiceTest.aSessionTheProcessLostRingsAgainWhereItWas; AlarmRingServiceTest.aStickyRestartWithNoIntentRingsAgain; AlarmRingServiceTest.restartingThePhoneDoesNotEndARingingAlarm.; AlarmRingServiceEdgeTest.aSessionOlderThanThreeHoursIsNotBroughtBack; ...aSessionJustUnderThreeHoursIsStillBroughtBack; ...aSessionFromTheFutureIsIgnoredInsteadOfRingingForever; ...theWatchdogWakingWithNothingSavedStopsWatching; ...aLostSessionWhoseEspereAlreadyEndedRingsAtOnce |  |
| F74 | Migração do banco (dados mantidos; amostras antigas removidas; pausa vira pulo) | FUNCIONA | DatabaseMigrationTest.upgradingFromVersion4KeepsTheUsersDataAndDropsTheSamples; DatabaseMigrationTest.upgradingFromVersion5TurnsAPausedDayIntoASkip. |  |
| F75 | Backup do Android (dados extraídos e backup completo) | FUNCIONA | AppConfigTest.theOldBackupRulesTakeOnlyTheDatabase; ...theNewBackupRulesTakeOnlyTheDatabaseForCloudAndPhoneToPhone; ...theManifestPointsAtBothBackupRuleFiles | Regras e manifesto provados. A restauração real do backup do Android não é testável na JVM. |
| F76 | Textos em português e inglês | FUNCIONA | StringsTest.everyStringExistsInBothLanguages; StringsTest.placeholdersMatch; StringsTest.dayArraysHaveSevenEntries; StringsTest.shortDayNamesFitADayKey; PortugueseUiTest.theStringsArePortuguese. |  |
| F77 | Tema escuro fixo, independente do sistema | FUNCIONA | AppConfigTest.theThemeHasOnlyADarkSchemeAndNeverAsksTheSystem; ...theBackgroundAndSurfacesAreDark; ...theSystemBarsAreDarkOnBothScreens | Provado por leitura do código-fonte e das cores. A renderização com o sistema claro não roda na JVM. |
| F78 | Canais de notificação (toque silencioso no canal do alarme, pergunta sem som) | FUNCIONA | AppSetupTest.theAlarmNotificationChannelRingsSilently; AppSetupTest.thePreAlarmChannelPopsUpWithoutASound; AppSetupTest.componentsAreWiredUp. |  |
| F79 | Componentes do manifesto registrados | FUNCIONA | AppSetupTest.componentsAreWiredUp. |  |
| F80 | Fuso e horário do Brasil nos testes de horário | FUNCIONA | AlarmTimingTest.useBrazilTime; AlarmTimingTest.oneOffLaterTodayRingsToday; AlarmTimingTest.oneOffAlreadyPastRingsTomorrow; AlarmTimingTest.theMinuteAnAlarmFiresIsNotScheduledAgain; AlarmTimingTest.weeklyAlarmThatAlreadyRangWaitsAWeek; AlarmTimingTest.nextTriggerDayMatchesTheTrigger.; AlarmTimingEdgeTest.theAlarmKeepsItsWallClockTimeAcrossTheSpringForwardChange; ...anAlarmInsideTheMissingHourStillRingsThatDay |  |
| F81 | Baixar o app | NÃO TESTÁVEL | SiteFilesTest.theDownloadConfigNamesTheSameFileThePagesLinkTo; ...bothLanguagesHaveTheSameDownloadHooks | JavaScript de navegador: não roda na JVM. Só a estrutura do HTML e os links são conferidos. |
| F82 | Trocar idioma PT/EN e redirecionar pelo idioma do navegador | NÃO TESTÁVEL | SiteFilesTest.theLanguageAttributeMatchesTheFolder; ...everyLocalLinkAndAssetResolvesToAFile | JavaScript de navegador: não roda na JVM. Só a estrutura do HTML e os links são conferidos. |
| F83 | Relógio em flip na capa | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F84 | Vídeo de demonstração com botão de play | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F85 | Demonstração do volume com rampa (interruptor) | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F86 | Demonstração do Espere (segurar o botão) | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F87 | Demonstração da calculadora (checar a resposta, teclado) | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F88 | Demonstração de escolha de som (dial de estações) | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F89 | Gráfico de exemplo "minutos até acordar" | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F90 | Animação Bom dia (12 minutos) | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F91 | Entrada das seções ao rolar | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F92 | Teste no Android (pedir acesso de teste por e-mail) | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F93 | Link de contato no rodapé | NÃO TESTÁVEL | nenhum | JavaScript de navegador: não roda na JVM. |
| F94 | Reagendar os alarmes ao mudar a hora do celular à mão | QUEBRADO | SessionBugTest.changingTheClockByHandRearmsTheAlarms (ignorado, BUG-2) | BUG-2: BootReceiver não escuta ACTION_TIME_CHANGED. |

## Contagem

- FUNCIONA: 79
- QUEBRADO: 2
- NÃO TESTÁVEL: 13

## Bugs encontrados

### BUG-1 (média): Espere durante a missão conta uma espera falsa

- Teste: `SessionBugTest.anEspereTapArrivingDuringTheMissionIsIgnored` (@Ignore).
- Onde: `AlarmRingService.onHoldOn` (app/src/main/java/com/wakewakeup/session/AlarmRingService.kt). Só confere `holdUntilMillis != null` e `currentAlarm == null`, não confere a tela.
- Reprodução: alarme tocando, tocar em "Começar missão" e receber logo depois `ACTION_HOLD_ON` (toque duplo ou intent atrasado). A tela é a da missão, mas `waits` vira 1, `holdUntilMillis` é definido, o som para e a notificação passa a dizer "Espere". `tickRinging` não roda na tela da missão, então esse Espere nunca termina; a espera falsa entra no histórico e nas estatísticas.
- Correção sugerida: em `onHoldOn`, sair se a tela da sessão não for `SessionScreen.RINGING`.

### BUG-2 (baixa): mudar a hora do celular à mão não reagenda os alarmes

- Teste: `SessionBugTest.changingTheClockByHandRearmsTheAlarms` (@Ignore).
- Onde: `BootReceiver.RESCHEDULE_ACTIONS` e a intent-filter do `AndroidManifest.xml`: cobrem boot, fuso, atualização e permissão de alarme exato, mas não `Intent.ACTION_TIME_CHANGED`.
- Reprodução: com um alarme diário às 07:00 armado, mudar o relógio do celular em uma hora nas configurações. O AlarmManager guarda o instante absoluto de antes, então o alarme toca em outro horário do relógio novo até o app ser aberto ou o celular reiniciar.
- Correção sugerida: adicionar `ACTION_TIME_CHANGED` ao filtro e ao conjunto de ações.

## Observações (não são bugs confirmados)

- Alarme de uma vez cujo horário passou com o celular desligado: `rescheduleAll` no boot arma o toque para o dia seguinte, em vez de avisar que perdeu. Aceitável, mas vale decidir.
- Só existe uma data de pulo por alarme (`skipDate`): não dá para pular dois dias seguidos, e pular outro toque sobrescreve o anterior.
- Se a missão estoura os 2 minutos, o progresso volta ao primeiro problema (`aTimedOutMissionStartsOverFromTheFirstProblem` documenta isso).
- O mapa dizia que o APK está versionado em `site/`; na verdade `wake-wake-up.apk` está no `.gitignore`. `SiteFilesTest` ignora o link do APK por isso.

## Notas de ambiente

- O caminho do projeto tem "Á" e o Gradle falha ao executar testes (`Could not execute test class`). Em worktrees, `gradlew`, `gradlew.bat` e `gradle-wrapper.jar` não vêm no git. Solução usada: copiar esses 3 arquivos e `local.properties` para o worktree (sem commitar) e mapear o worktree numa letra de unidade (`subst T: <worktree>`), rodando o Gradle em `T:\`. A letra `W:` já era de outro agente.
