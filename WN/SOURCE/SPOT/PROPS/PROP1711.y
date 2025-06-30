



%{

/* .......... Declarations C ........*/
#pragma warning (disable:4786)
#include "../../../INCLUDE/const.h"
#include "../../../INCLUDE/SCONSSPOT.h"
#include "../../../INCLUDE/struct.h"
#include "../../../INCLUDE/SSTRUCTSPOT.h"
#include "../../../INCLUDE/var_ext.h"
#include "../../../INCLUDE/SVAR_EXTSPOT.h"
#include "../../../INCLUDE/macros.h"

/***************** Variables *******************/

struct Token_Domains TD; 
char*   Ptr_Cur=NULL;
int     Lim_Pos=0;

/**********************************************/

/**************** flex functions ************************/
extern int yylex();
extern FILE *yyin;
void yyerror(char *s){  printf("parsing error: %s\n", s); } 
int Propwrap(){ return (1); }
extern void   flush_buffer();
/*******************************************************/


/************** extern functions needed to compute local partitions *******/
extern int get_obj_id();
extern void* ecalloc();
extern void* emalloc();
extern int initialize ();
extern int GetIndObj();
extern  TYPE_P** ComputeLocalPartitions();
extern  TYPE_P** FindDisjParts2();
extern char*** GetMatRepOfClObj();
extern int initialize () ;
extern void Afficher();
/************************************************************************/

int EXISTE_PLACE(char* id){
int i=0;
 
    while (i<npl){
      if (strcmp(tabp[i].place_name,id)==0) return i;
       i++;  
     }
    return NOT_PRESENT; 
} 
 
          
%}

/*........... Declarations Yacc .....*/

%union { 	

  int j;
  char* s;
  struct CTOKEN * tk;
  struct TREE_NODE * nd;
}
/*-----------------------------------------les tokens----------------------------------*/

%token <j> IDENT 
%token <s> ID
%token  DL PRS INT EDL EPRS EIN  NOT EQ LT GT OR AND DECOL MINUS IDEN 
/*-----------------------------------------les <> types----------------------------------*/
%type <s>  VARS
%type <tk> CONST FONC
%type <tk>  PROJ
%type <j>  OP
%type <nd> EXPR1 EXPR2 EXPR3 ATOMIC  FORMULES  

%left IDENT 
%left OR
%left AND 
%left '('  ')'

/*----------------------------------------le debut du parse-----------------------------------*/
%start FORMULES


%%

OP              : EQ {$$=EQOP;}| NOT EQ {$$=NEQOP;}  | LT{$$=LOP;}      | LT EQ{$$=LQOP;}  | NOT LT{$$=NLOP;} | NOT LT EQ{$$=NLQOP;}
                | GT {$$=GOP;} | GT EQ  {$$=GQOP;}   | NOT GT{$$=NGOP;} | NOT GT EQ{$$=NGQOP;} ;
 
VARS            : ID {
                       int Cl;
                       int v=GetIndObj($1,MTCL,&Cl);
                       if(v==UNKNOWN) {
			 printf("\n L'objet :%s n'est pas une couleur valide du r�seau v�rifi� \n",$1); 
                         return(1);
                       }
                       TD.Token=realloc(TD.Token,(TD.NbCl+1)*sizeof(int));
                       TD.Domain=realloc(TD.Domain,(TD.NbCl+1)*sizeof(int));
		       TD.Token[TD.NbCl]=v ; TD.Domain[TD.NbCl]=Cl; TD.NbCl++ ;
                
                      } ',' VARS  |
                  ID {
			int Cl;
			int v=GetIndObj($1,MTCL,&Cl);
			if(v==UNKNOWN) {
			  printf("\n  L'objet :%s n'est pas une couleur valide du r�seau v�rifi� \n",$1); 
			  return(1);
			}
			TD.Token=realloc(TD.Token,(TD.NbCl+1)*sizeof(int));
			TD.Domain=realloc(TD.Domain,(TD.NbCl+1)*sizeof(int));
			TD.Token[TD.NbCl]=v ; TD.Domain[TD.NbCl]=Cl; TD.NbCl++ ;                   
                     };

PROJECTING      : IDEN  {
                           TD.Token=realloc(TD.Token,(TD.NbCl+1)*sizeof(int));
			   TD.Token[TD.NbCl]=PROJECT ; TD.NbCl++ ;   
			}  |
                  MINUS { 
                           TD.Token=realloc(TD.Token,(TD.NbCl+1)*sizeof(int));
			   TD.Token[TD.NbCl]=ELIMINATE ; TD.NbCl++ ;  
			 } ;

PERS            : PROJECTING ',' PERS  | PROJECTING  ;

PROJ            :  {TD.NbCl=0;TD.Token=NULL;TD.Domain=NULL; }
                   '[' PERS ']' '('ID')' 
                      {  
			int i;
		        if ((i=EXISTE_PLACE($6))==NOT_PRESENT) 
			  {
			    printf("\n '%s' n'est pas une place valide du r�seau v�rifi� \n", $6);
			    return(1);
			  }
                        if(TD.NbCl==1 && TD.Token[0]==ELIMINATE) 
			  {
			    printf("\n La d�colorisation n'est pas autoris�e pour la place '%s'  \n ",$6);
			    return(1);
			  }
                        if(TD.NbCl!=tabp[i].comp_num)
			  {
                            printf("\n La fonction de projection et le domaine de la place '%s' ne sont pas compatible  : \n ",$6);
			    return (1);
			  }
		      
                        $$=(CToken_p)emalloc(sizeof(CToken));
		        $$->mult=i;
			($$->tok_dom).Token=TD.Token;  
			($$->tok_dom).Domain=NULL;   
			for(i=0,($$->tok_dom).NbCl=0;i<TD.NbCl;i++)
			  if(($$->tok_dom).Token[i]==PROJECT)
			  {
			    ($$->tok_dom).Domain=realloc(($$->tok_dom).Domain,((($$->tok_dom).NbCl)+1)*sizeof(int));
                            ($$->tok_dom).Domain[ ($$->tok_dom).NbCl]=tabp[$$->mult].dominio[i]; 
			    ($$->tok_dom).NbCl++;
			  }  
			$$->next=NULL; 
		     
		      };

CONST           : CONST OR CONST {   int i; 
                                     CToken_p ptr=$1;
                                     if(($1->tok_dom).NbCl!=($3->tok_dom).NbCl )
				       {
                                         printf("\n Une ou plusieurs formules expriment une incompatibilit� entre les domaines de couleurs des jetons : \n ");
			                 return(1);  
				       }
				     for(i=0; i<($1->tok_dom).NbCl;i++)
				       if( ($1->tok_dom).Domain[i]!= ($3->tok_dom).Domain[i] )
				       {
					  printf("\n Une ou plusieurs formules expriment une incompatibilit� entre les domaines de couleurs des jetons : \n ");
					  return(1);  
				       }
				     while(ptr->next!=NULL) ptr=ptr->next;
                                      ptr->next=$3; $$=$1;
                                  } |
                  {TD.NbCl=0;TD.Token=NULL;TD.Domain=NULL;}  
                  IDENT LT VARS GT {
		                     $$= (CToken_p)emalloc(sizeof(CToken));
				     if($2==0)
				       {
					 printf("\n La valeur '0' n'est pas une multiplicit� valide pour un jeton \n");
					 return(1);
                                       }
				     $$->mult=$2;
				     ($$->tok_dom).Token=TD.Token;     
				     ($$->tok_dom).Domain=TD.Domain;
				     ($$->tok_dom).NbCl=TD.NbCl;
				     $$->next=NULL;
		                   } |
                   {TD.NbCl=0;TD.Token=NULL;TD.Domain=NULL;}  
                   LT VARS GT {      
		                     $$=(CToken_p)emalloc(sizeof(CToken));
				     $$->mult=1;
				     ($$->tok_dom).Token=TD.Token;     
				     ($$->tok_dom).Domain=TD.Domain;
				     ($$->tok_dom).NbCl=TD.NbCl;
				     $$->next=NULL;
 
		            } 
                  ; 

FONC            : ID {
		      int i;
		      if ((i=EXISTE_PLACE($1))==NOT_PRESENT) {
			 
			   printf("\n '%s' n'est pas une place valide du r�seau v�rifi� \n", $1);
			   return(1);
		       }
                        $$=(CToken_p)emalloc(sizeof(CToken));
		        $$->mult=i; 
			($$->tok_dom).Token=NULL;   
			($$->tok_dom).Domain=tabp[i].dominio;
			($$->tok_dom).NbCl=tabp[i].comp_num;
			$$->next=NULL;
		
                     };

              
                  

FORMULES        : ID ':' ATOMIC {   TreeNode_p pt1=$3;
                                     int i;
				     for(i=0;i<NbProp;i++)
                                       if(strcmp(PROPOSITIONS[i].id,$1)==0 )
					 {
					   printf("\n La propostion atomique '%s' est doublement d�clar�e  \n",$1);
					   return(1);
					 }
				     PROPOSITIONS=realloc(PROPOSITIONS,(NbProp+1)*sizeof(PropAtomic));
				     PROPOSITIONS[NbProp].id=$1;
         			     PROPOSITIONS[NbProp].prop=$3;
				     NbProp++;
				     $$=NULL;
                                }';' FORMULES  |{$$=NULL;} ;

ATOMIC          :  EXPR3 OP EXPR3  {
                                        int i;
		                        $$= (struct TREE_NODE*)emalloc(sizeof(struct TREE_NODE));
                                        $$->op=$2;
                                        $$->type=NOTYPE;
						                       
					if((($1->FONCT)->tok_dom).NbCl!=(($3->FONCT)->tok_dom).NbCl)
					  {
					    printf("\n les domaines de couleurs des sous-expressions sont imcompatibles. \n ");
			                    return(1);     
					  }
				        
					for( i=0; i<(($3->FONCT)->tok_dom).NbCl;i++)
					  if( (($1->FONCT)->tok_dom).Domain[i]!= (($3->FONCT)->tok_dom).Domain[i] )
					    {
					      printf("\n les domaines de couleurs des sous-expressions sont imcompatibles.  \n ");
					      return(1);  
					    }
					TreeNode_p pt1,pt3;
                                        $$->FONCT=$3->FONCT;
					$$->filsg=pt1=$1;
					$$->filsd=pt3=$3;
				
					if($1->PART_MAT==NULL && $3->PART_MAT==NULL ) { $$->PART_MAT=NULL; $$->NbElPM=NULL;}
					else
					  if($1->PART_MAT==NULL && $3->PART_MAT!=NULL){$$->PART_MAT=$3->PART_MAT; $$->NbElPM=$3->NbElPM; }
					  else 
					    if($1->PART_MAT!=NULL && $3->PART_MAT==NULL){ $$->PART_MAT=$1->PART_MAT; $$->NbElPM=$1->NbElPM; }
					    else 
					      { 
						$$->PART_MAT=FindDisjParts2((($1->FONCT)->tok_dom).Domain,
									    (($1->FONCT)->tok_dom).NbCl,$1->PART_MAT,
									     $1->NbElPM, $3->PART_MAT,$3->NbElPM);
                                                $$->NbElPM=$3->NbElPM;
						$3->PART_MAT=NULL;
						$3->NbElPM=NULL;
					      }
						
		  }; 



EXPR3           : FONC               {  
		                        $$= (struct TREE_NODE*)emalloc(sizeof(struct TREE_NODE));
                                        $$->op=NOP;
                                        $$->type=IDE;
					$$->FONCT=$1;
					$$->PART_MAT=NULL;
					$$->NbElPM=NULL;
					$$->filsg=$$->filsd=NULL;
                                      }|
                 CONST              { 
                                       $$= (struct TREE_NODE*)emalloc(sizeof(struct TREE_NODE));
                                       $$->op=NOP;
                                       $$->type=TOK;
				       $$->FONCT=$1;
				       $$->filsg=$$->filsd=NULL; 
				       $$->PART_MAT=ComputeLocalPartitions( MTCL,($1->tok_dom).Domain,($1->tok_dom).NbCl,$1, &($$->NbElPM));
				 
		                     } |
                 PROJ               { 
                                       $$= (struct TREE_NODE*)emalloc(sizeof(struct TREE_NODE));
                                       $$->op=NOP;
                                       $$->type=PRO;
				       $$->FONCT=$1;
        			       $$->PART_MAT=NULL;
				       $$->NbElPM=NULL;
				       $$->filsg=$$->filsd=NULL; 
                                    }  |
                 '(' EXPR3 ')'      {$$=$2; }|
		 EXPR3 AND EXPR3   {
		                       int i;
		                        $$= (struct TREE_NODE*)emalloc(sizeof(struct TREE_NODE));
                                        $$->op=ANDOP;
                                        $$->type=NOTYPE;
					$$->FONCT=NULL;
					if((($1->FONCT)->tok_dom).NbCl!=(($3->FONCT)->tok_dom).NbCl)
					  {
					    printf("\n Des domaines de couleurs des sous-expressions sont imcompatibles. \n ");
			                    return(1);     
					  }
					for( i=0; i<(($1->FONCT)->tok_dom).NbCl;i++)
					  if( (($1->FONCT)->tok_dom).Domain[i]!= (($3->FONCT)->tok_dom).Domain[i] )
					    {
					      printf("\n Des domaines de couleurs des sous-expressions sont imcompatibles. \n ");
					      return(1);  
					    }
					TreeNode_p pt1,pt3;
					$$->FONCT=$1->FONCT;
					$$->filsg=pt1=$1;
					$$->filsd=pt3=$3;
					
					if($1->PART_MAT==NULL && $3->PART_MAT==NULL ) { $$->PART_MAT=NULL; $$->NbElPM=NULL;}
					else
					  if($1->PART_MAT==NULL && $3->PART_MAT!=NULL){$$->PART_MAT=$3->PART_MAT; $$->NbElPM=$3->NbElPM; }
					  else 
					    if($1->PART_MAT!=NULL && $3->PART_MAT==NULL){ $$->PART_MAT=$1->PART_MAT; $$->NbElPM=$1->NbElPM; }
					    else 
					      { 
						$$->PART_MAT=FindDisjParts2((($1->FONCT)->tok_dom).Domain,
									    (($1->FONCT)->tok_dom).NbCl,$1->PART_MAT,
									     $1->NbElPM, $3->PART_MAT,$3->NbElPM);
                                                $$->NbElPM=$3->NbElPM;
						$3->PART_MAT=NULL;
						$3->NbElPM=NULL;
					      }
				   
		                    }|
		 EXPR3 OR  EXPR3   {  
		                        int i;
		                        $$= (struct TREE_NODE*)emalloc(sizeof(struct TREE_NODE));
                                        $$->op=OROP;
                                        $$->type=NOTYPE;
					
					if((($1->FONCT)->tok_dom).NbCl!=(($3->FONCT)->tok_dom).NbCl)
					  {
					    printf("\n Des domaines de couleurs des sous-expressions sont imcompatibles. \n ");
			                    return(1);     
					  }
					for( i=0; i<(($1->FONCT)->tok_dom).NbCl;i++)
					  if( (($1->FONCT)->tok_dom).Domain[i]!= (($3->FONCT)->tok_dom).Domain[i] )
					    {
					      printf("\n Des domaines de couleurs des sous-expressions sont imcompatibles. \n ");
					      return(1);  
					    }
					$$->FONCT=$1->FONCT;
					$$->filsg=$1;
					$$->filsd=$3;
					
					if($1->PART_MAT==NULL && $3->PART_MAT==NULL ) { $$->PART_MAT=NULL; $$->NbElPM=NULL;}
					else
					  if($1->PART_MAT==NULL && $3->PART_MAT!=NULL){ $$->PART_MAT=$3->PART_MAT; $$->NbElPM=$3->NbElPM; }
					  else 
					    if($1->PART_MAT!=NULL && $3->PART_MAT==NULL){ $$->PART_MAT=$1->PART_MAT; $$->NbElPM=$1->NbElPM; }
					    else 
					      {
					        $$->PART_MAT=FindDisjParts2((($1->FONCT)->tok_dom).Domain,(($1->FONCT)->tok_dom).NbCl,$1->PART_MAT,
									    $1->NbElPM, $3->PART_MAT,$3->NbElPM);
                                                $$->NbElPM=$3->NbElPM;
						$3->PART_MAT=NULL;
						$3->NbElPM=NULL;
					      }
		                    };

EXPR1           : DECOL '('FONC ')' {}|
                 '(' EXPR1 ')'      {}|
                  EXPR1 OR  EXPR1   {};

EXPR2           : DECOL '('FONC ')' {}|
                  IDENT             {}|  
                 '(' EXPR2 ')'      {}|
                  EXPR2 OR  EXPR2   {};
                  
%%

int prop_parser(FILE* f){

char c;
int i=0;
 
   Ptr_Cur=(char*)malloc(STR_SIZE*sizeof(char));
     
   while((c=fgetc(f))!=EOF)  
     {
       Ptr_Cur[i]=c; 
       i++;
     }

   Ptr_Cur[i]='\0';
   Lim_Pos=(int)Ptr_Cur+strlen(Ptr_Cur);   

   if(i==0)
     {
       free(Ptr_Cur);
       printf("\n Le fichier des propositions atomiques est vide \n");
       return 1;
     }
   i=yyparse();
   
   _CONS_=calloc(NbProp,sizeof(int)); 

  return i;
}















