# TrickierTrials – TheCivia fork

Fork pluginu [TrickierTrials](https://github.com/t14d3/TrickierTrials) (t14d3) pro survival server TheCivia. Upravuje trial chambers:

- **Silnější moby z trial spawnerů** podle výbavy hráčů v okolí (4 úrovně: víc životů, poškození, brnění, meče). Dropy těchto mobů jsou omezené na breeze rod, rotten flesh, kosti a šípy, takže výbava mobů nejde vyfarmit.
- **Cooldown ominous vaultů:** stejný ominous vault jde po `trial-vault-reset-time` (ms) vybrat znovu. Normální vaulty zůstávají vanilla.
- **Ochrana chamberu** (volitelně): rozpad položených bloků, obnova rozbitých bloků ze seznamu, mining fatigue.
- Glowing a fire resistance pro trial moby, easter egg breeze.

## Větve
- `master`: původní kód s naší úpravou cooldownu ominous vaultu (NMS, Gradle + paperweight 1.21.1).
- `fix/memory-leak-and-reload-perm`: únik paměti u položených bloků (mapa rostla až do restartu), strukturní lookupy jen když je funkce zapnutá, nastavení se načítají při reloadu, `/trickiertrials` vyžaduje `trickiertrials.admin` (op).
- `fix/vault-api-26.3` (navazuje na předchozí, **verze 1.4.0-civia**):
  - cooldown vaultů přes Paper API (`org.bukkit.block.Vault`, PDC `TileState`), **bez NMS a reflexe**,
  - názvy atributů pro 26.3 s kontrolou `null` (breeze dřív shodil zpracování spawnu a jeho dropy se nefiltrovaly),
  - Maven build proti Paper API 26.3 / Java 25, Gradle odstraněný.

## Config (výběr)
| Klíč | Výchozí | K čemu |
|---|---|---|
| `strengthen-trial-mobs` | `true` | silnější moby podle výbavy |
| `trial-vault-reset-time` | `86400000` | cooldown ominous vaultu v ms, `-1` vypne |
| `vault-locked-message` | – | MiniMessage zpráva při zamčeném vaultu, `<time>` = zbývající čas |
| `blocks-to-protect`, `regenerate-broken-blocks`, `regenerate-delay` | | obnova rozbitých bloků |
| `decay-placed-blocks`, `decay-delay` | | rozpad položených bloků |

## Build
Na větvi `fix/vault-api-26.3`:
```
mvn clean package      # target/TrickierTrials-1.4.0-civia.jar
```
