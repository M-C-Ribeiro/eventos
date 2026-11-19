package com.eventhub.tenant;

/**
 * Guarda o tenant (organização) da requisição atual.
 * É preenchido pelo filtro JWT e lido pelo Hibernate para isolar os dados.
 */
public final class TenantContext {

    /** Valor usado quando não há tenant definido: nenhum registro tem esse id, então nada é retornado. */
    public static final Long NO_TENANT = -1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Executa um bloco no contexto de um tenant específico, restaurando o anterior ao final. */
    public static <T> T callAs(Long tenantId, java.util.function.Supplier<T> action) {
        Long previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
