// Typy uzywane wylacznie przez modul admina.

/** Liczniki kont z /api/admin/stats (UserAccountStatisticsDto). */
export interface UserAccountStatistics {
  totalAccounts: number;
  arboristAccounts: number;
  officialAccounts: number;
  adminAccounts: number;
  accountsAwaitingEmailConfirmation: number;
  accountsLockedByAdmin: number;
}
