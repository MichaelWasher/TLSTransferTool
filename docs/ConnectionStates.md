Client Request Messages:
- LIST
- GET \<filename>
    
Server Response Messages:
- GET
    - SUCCESS GET<br>\<serialised file>
    - DENIED GET
    - FAILED GET
- LIST
    - SUCCESS LIST<br> \<list of files> 
    - DENIED LIST
    - FAILED LIST