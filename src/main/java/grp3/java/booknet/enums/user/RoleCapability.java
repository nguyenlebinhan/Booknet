package grp3.java.booknet.enums.user;
public enum RoleCapability {
 PROFILE_UPDATE("Edit own profile","Update personal contact details and account preferences.","Personal account & borrowing"),
 LOAN_REQUEST("Request loans","Submit borrowing requests for available catalog titles.","Personal account & borrowing"),
 RESERVE("Reserve titles","Place and manage personal title reservations.","Personal account & borrowing"),
 RENEW("Request renewals","Ask to extend eligible loans; library policies still apply.","Personal account & borrowing"),
 CATALOG_EDIT("Edit book metadata","Update titles, authors, categories and catalog descriptions.","Catalog"),
 COPY_MANAGE("Manage physical copies","Add, move or remove individual book copies.","Catalog"),
 BOOK_RETIRE("Retire titles","Remove catalog titles from active circulation.","Catalog"),
 MEMBER_READ("View member service records","View member contact details, loans, reservations and fine balances.","Operations"),
 MEMBER_CARD_ACTIVATE("Activate member cards","Verify identity and activate library cards.","Operations"),
 CIRCULATION("Process loans & returns","Check books out and record returned copies.","Operations"),
 FINE_PAYMENT("Record finalized fine payments","Record completed payments only; this does not allow fine waivers.","Operations"),
 INVENTORY_REPORT("View inventory reports","Review stock levels and catalog inventory summaries.","Reports"),
 CIRCULATION_REPORT("View circulation reports","Access borrowing activity and circulation trends.","Reports"),
 SERVICE_EMAIL("Send operational emails","Send borrowing, renewal, return and reservation service notices.","Reports"),
 USER_READ("View user accounts","Review member and staff accounts.","Administration"),
 USER_ACCESS_UPDATE("Manage accounts","Create, suspend or update user accounts.","Administration"),
 ROLE_MANAGE("Manage roles & permissions","Review role access and permission assignments.","Administration"),
 POLICY_MANAGE("Configure policies & waive fines","Control library policies and authorize fine waivers.","Administration"),
 AUDIT_READ("View audit log","Review recorded administrative activity.","Administration");
 private final String label,description,group;
 RoleCapability(String label,String description,String group){this.label=label;this.description=description;this.group=group;}
 public String getLabel(){return label;} public String getDescription(){return description;} public String getGroup(){return group;}
 public boolean administrative(){return group.equals("Administration");}
}